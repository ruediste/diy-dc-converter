#include "control_main.h"
#include "interface/messages.h"
#include <string.h>
#include "main.h"
#include "USB_DEVICE/App/usbd_cdc_if.h"

namespace ControlMain
{
    void dummyControlHandler()
    {
    }
    void dummyMainHandler()
    {
    }

    void (*controlHandler)() = dummyControlHandler;
    void (*mainHandler)() = dummyMainHandler;
}

namespace PWMMode
{
    void handle(PWMModeConfigMessage *config)
    {
        htim1.Instance->ARR = config->reload;
        htim1.Instance->CCR1 = config->compare;
        htim1.Instance->PSC = config->prescale;

        if (config->running)
        {
            HAL_TIM_Base_Start(&htim1);
            HAL_TIM_PWM_Start(&htim1, TIM_CHANNEL_1);
        }
        else
        {
            HAL_TIM_PWM_Stop(&htim1, TIM_CHANNEL_1);
        }
    }
}

namespace DcmBoostPid
{
    const int adcBufCount = 9;
    uint16_t adcBuf[adcBufCount];
    int controlCount;

    void control()
    {
        controlCount++;
    }

    uint32_t lastStatusSend = 0;
    void main()
    {
        auto now = HAL_GetTick();
        if (now - lastStatusSend > 500)
        {
            lastStatusSend = now;
            uint8_t buf[sizeof(DcmBoostPidStatusMessage) + 1];
            buf[0] = (uint8_t)MessageType::DcmBoostPidStatusMessage;
            DcmBoostPidStatusMessage *msg = (DcmBoostPidStatusMessage *)(buf + 1);
            uint16_t adc0 = 0;
            int adc0Count = 0;
            uint16_t adc1 = 0;
            int adc1Count = 0;
            __disable_irq();
            int offset = adcBufCount - DMA2_Stream2->NDTR;
            for (int i = 1; i < adcBufCount; i++)
            {
                auto value = adcBuf[(i + offset) % adcBufCount];
                if ((i % 2) == 0)
                {
                    adc0 += value + 1;
                    adc0Count++;
                }
                if ((i % 2) == 1)
                {
                    adc1 += value;
                    adc1Count++;
                }
            }
            __enable_irq();
            msg->adc0 = adc0 / adc0Count;
            msg->adc1 = adc1 / adc1Count + controlCount;
            CDC_Transmit_FS(buf, sizeof(buf));
        }
    }

    void handle(DcmBoostPidConfigMessage *config)
    {
        htim1.Instance->ARR = config->reloadPwm;
        htim1.Instance->PSC = config->prescalePwm;
        htim1.Instance->CCR1 = config->reloadPwm / 8;
        htim1.Instance->CCR4 = 10;

        htim9.Instance->ARR = config->reloadCtrl;
        htim9.Instance->PSC = config->prescaleCtrl;

        ControlMain::controlHandler = control;
        ControlMain::mainHandler = main;
        controlCount = 0;

        if (config->running)
        {
            HAL_TIM_Base_Start(&htim1);
            HAL_TIM_PWM_Start(&htim1, TIM_CHANNEL_1);
            HAL_TIM_PWM_Start(&htim1, TIM_CHANNEL_4);

            HAL_TIM_Base_Start_IT(&htim9);

            for (int i = 0; i < adcBufCount; i++)
                adcBuf[i] = 0;

            HAL_ADC_Start_DMA(&hadc1, (uint32_t *)adcBuf, sizeof(adcBuf));
        }
        else
        {
            HAL_TIM_PWM_Stop(&htim1, TIM_CHANNEL_1);
        }
    }
}

namespace ControlMain
{
    uint8_t aggregationBuffer[maxMessageSize + 1];
    uint32_t index = 0;

    void reset()
    {
        HAL_TIM_Base_Stop(&htim1);
        HAL_TIM_Base_Stop(&htim9);
        controlHandler = dummyControlHandler;
        mainHandler = dummyMainHandler;
    }

    void handleMessage(uint8_t messageType)
    {
        reset();
        switch ((MessageType)messageType)
        {
        case MessageType::PWMModeConfigMessage:
            PWMMode::handle((PWMModeConfigMessage *)(aggregationBuffer + 1));
            break;
        case MessageType::DcmBoostPidConfigMessage:
            DcmBoostPid::handle((DcmBoostPidConfigMessage *)(aggregationBuffer + 1));
            break;

        default:
            break;
        }
    }

    void receiveData(uint8_t *buf, uint32_t *len)
    {
        uint32_t i = 0;
        while (i < *len)
        {
            uint32_t remainingAggBuffer = maxMessageSize + 1 - index;
            uint32_t remainingBuf = (*len) - i;
            uint32_t remaining = remainingAggBuffer > remainingBuf ? remainingBuf : remainingAggBuffer;
            memcpy(aggregationBuffer + index, buf + i, remaining);
            index += remaining;
            i += remaining;
            if (index > 0)
            {
                uint8_t messageType = aggregationBuffer[0];

                uint32_t messageSize = messageSizes[messageType];
                if (index >= messageSize + 1)
                {
                    // complete message is in aggregation buffer
                    handleMessage(messageType);

                    // shrink aggregation buffer
                    if (index > messageSize + 1)
                        memmove(aggregationBuffer, aggregationBuffer + index, index - (messageSize + 1));
                    index -= messageSize + 1;
                }
            }
        }
    }

    void onControlLoop()
    {
        controlHandler();
    }

    void controlMain()
    {
        mainHandler();
    }
}

void receiveData(uint8_t *buf, uint32_t *len)
{
    ControlMain::receiveData(buf, len);
}

void onControlLoop()
{
    ControlMain::onControlLoop();
}

void controlMain()
{
    ControlMain::controlMain();
}