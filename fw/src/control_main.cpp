#include "control_main.h"
#include "interface/messages.h"
#include <string.h>
#include "main.h"
#include "USB_DEVICE/App/usbd_cdc_if.h"
#include <algorithm>

template <typename T>
struct MessageBuffer
{
    uint8_t type;
    T msg;

    MessageBuffer(MessageType type)
    {
        this->type = (uint8_t)type;
    }

    uint8_t *bytes()
    {
        return (uint8_t *)this;
    }
};

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

    void reset()
    {
        HAL_ADC_Stop_DMA(&hadc1);
        HAL_TIM_Base_Stop(&htim1);
        HAL_TIM_Base_Stop(&htim9);
        controlHandler = dummyControlHandler;
        mainHandler = dummyMainHandler;
        // set to 10kHz
        TIM1->PSC = 0;
        TIM1->ARR = 8400;
        // 1% duty
        TIM1->CCR1 = 84;
        // trigger ADC conversion at start of cycle
        TIM1->CCR3 = 0;

        // set to 10kHz
        TIM9->PSC = 0;
        TIM9->ARR = 8400;
    }

    const int adcChannelCount = 2;
    const int adcBufCount = adcChannelCount * 4;
    uint16_t adcBuf[adcBufCount];

    void startStopPWMOutput(bool running)
    {
        if (running)
        {
            HAL_TIM_PWM_Start(&htim1, TIM_CHANNEL_1);
        }
        else
        {
            HAL_TIM_PWM_Stop(&htim1, TIM_CHANNEL_1);
        }
    }

    void setAdcSampleCycles(uint8_t value)
    {
        ADC1->SMPR2 &= ~0x1FF;
        ADC1->SMPR2 |= value | (value << 3);
    }

    void startTimers()
    {
        HAL_TIM_Base_Start(&htim1);
        HAL_TIM_OC_Start(&htim1, TIM_CHANNEL_3);

        HAL_TIM_Base_Start_IT(&htim9);

        for (int i = 0; i < adcBufCount; i++)
            adcBuf[i] = 0;

        HAL_ADC_Start_DMA(&hadc1, (uint32_t *)adcBuf, adcBufCount);
    }

    void readADCValues(int nAvg, uint16_t adcValues[adcChannelCount])
    {
        int adcCounts[adcChannelCount] = {0};
        for (int i = 0; i < adcChannelCount; i++)
            adcValues[i] = 0;

        // do this quickly in one go
        __disable_irq();
        // find the offset in the buffer where the next value will be written by DMA
        // we'll skip this offset when copying
        int offset = adcBufCount - DMA2_Stream0->NDTR;
        for (int i = adcBufCount - nAvg * adcChannelCount; i < adcBufCount; i++)
        {
            int bufIdx = (i + offset) % adcBufCount;
            auto value = adcBuf[bufIdx];
            adcValues[bufIdx % adcChannelCount] += value;
            adcCounts[bufIdx % adcChannelCount]++;
        }
        __enable_irq();

        for (int i = 0; i < adcChannelCount; i++)
            adcValues[i] /= adcCounts[i];
    }
}

namespace PWMMode
{
    uint32_t lastStatusSend = 0;
    void main()
    {
        auto now = HAL_GetTick();
        if (now - lastStatusSend > 500)
        {
            lastStatusSend = now;

            {
                MessageBuffer<DebugMessage> buf(MessageType::DebugMessage);
                uint32_t *buf2 = (uint32_t *)buf.msg.data;
                buf2[0] = ADC1->SMPR1;
                buf2[1] = ADC1->SMPR2;
                buf2[2] = 0;
                buf2[3] = 0;
                while (CDC_Transmit_FS(buf.bytes(), sizeof(buf)) == USBD_BUSY)
                    ;
            }
        }
    }

    void handle(PWMModeConfigMessage *config)
    {
        TIM1->ARR = config->reload;
        TIM1->CCR1 = config->compare;
        TIM1->CCR3 = config->adcTrigger;
        TIM1->PSC = config->prescale;
        ControlMain::mainHandler = main;

        ControlMain::setAdcSampleCycles(config->adcSampleCycles);
        ControlMain::startTimers();
        ControlMain::startStopPWMOutput(config->running);
    }
}

namespace SimpleControlMode
{

    uint16_t pwmMaxCompare;
    uint16_t targetAdc;
    float dutyChangeStep;
    float duty = 0;

    void control()
    {
        uint16_t adcValues[ControlMain::adcChannelCount];
        ControlMain::readADCValues(2, adcValues);
        if (adcValues[0] < targetAdc)
            duty += dutyChangeStep;
        else
            duty -= dutyChangeStep;
        if (duty < 0)
        {
            duty = 0;
        }
        uint16_t compareValue = duty * TIM1->ARR;
        if (compareValue > pwmMaxCompare)
        {
            compareValue = pwmMaxCompare;
            duty = pwmMaxCompare / (double)TIM1->ARR;
        }
        TIM1->CCR1 = compareValue;
    }

    uint32_t lastStatusSend = 0;
    void main()
    {
        auto now = HAL_GetTick();
        if (now - lastStatusSend > 500)
        {
            lastStatusSend = now;
            {
                MessageBuffer<SimpleControlStatusMessage> buf(MessageType::SimpleControlStatusMessage);
                buf.msg.compareValue = TIM1->CCR1;
                buf.msg.duty = duty;
                while (CDC_Transmit_FS(buf.bytes(), sizeof(buf)) == USBD_BUSY)
                    ;
            }
        }
    }

    void handle(SimpleControlConfigMessage *config)
    {
        TIM1->ARR = config->pwmReload;
        TIM1->PSC = config->pwmPrescale;
        TIM1->CCR1 = 0;
        TIM1->CCR3 = 10;

        TIM9->PSC = config->ctrlPrescale;
        TIM9->ARR = config->ctrlReload;

        pwmMaxCompare = config->pwmMaxCompare;
        dutyChangeStep = config->dutyChangeStep;
        targetAdc = config->targetAdc;
        // duty = 0;

        ControlMain::setAdcSampleCycles(config->adcSampleCycles);
        ControlMain::controlHandler = control;
        ControlMain::mainHandler = main;

        ControlMain::startTimers();
        ControlMain::startStopPWMOutput(config->running);
    }
}

namespace PidControlMode
{

    PidControlConfigMessage config;

    float duty = 0;
    float integral;
    float kIInv;
    float lastError;

    uint32_t lastStatusSend = 0;

    void control()
    {
        uint16_t adcValues[ControlMain::adcChannelCount];
        ControlMain::readADCValues(1, adcValues);

        auto outAvg = adcValues[0];
        int32_t error = config.targetAdc - outAvg;
        float errorF = error * 8. / 1600.;

        integral += errorF;
        integral = std::max(-kIInv, std::min(kIInv, integral));

        float diff = errorF - lastError;

        duty = errorF * config.kP + integral * config.kI + diff * config.kD;

        duty = std::max(0.0f, std::min(duty, 1.0f));
        lastError = errorF;

        uint16_t compareValue = duty * TIM1->ARR;
        if (compareValue > config.pwmMaxCompare)
        {
            compareValue = config.pwmMaxCompare;
            duty = config.pwmMaxCompare / (double)TIM1->ARR;
        }
        TIM1->CCR1 = compareValue;
    }

    void main()
    {
        auto now = HAL_GetTick();
        if (now - lastStatusSend > 500)
        {
            lastStatusSend = now;

            {
                MessageBuffer<PidControlStatusMessage> buf(MessageType::PidControlStatusMessage);

                uint16_t adcValues[ControlMain::adcChannelCount];
                ControlMain::readADCValues(2, adcValues);
                buf.msg.compareValue = TIM1->CCR1;
                buf.msg.duty = duty;
                while (CDC_Transmit_FS(buf.bytes(), sizeof(buf)) == USBD_BUSY)
                    ;
            }
            {
                MessageBuffer<DebugMessage> buf(MessageType::DebugMessage);
                uint32_t *buf2 = (uint32_t *)buf.msg.data;
                buf2[0] = ADC1->SR;
                buf2[1] = ADC1->CR1;
                buf2[2] = DMA2_Stream0->NDTR;
                buf2[3] = TIM1->SR;
                while (CDC_Transmit_FS(buf.bytes(), sizeof(buf)) == USBD_BUSY)
                    ;
                ADC1->SR = 0;
                TIM1->SR = 0;
                // ADC1->CR2 |= ADC_CR2_JSWSTART;
            }
        }
    }

    void handle(PidControlConfigMessage *config)
    {
        TIM1->ARR = config->pwmReload;
        TIM1->PSC = config->pwmPrescale;
        TIM1->CCR1 = 0;
        TIM1->CCR3 = 10;

        TIM9->PSC = config->ctrlPrescale;
        TIM9->ARR = config->ctrlReload;

        PidControlMode::config = *config;
        kIInv = 1 / config->kI;
        lastError = 0;
        integral = 0;

        ControlMain::setAdcSampleCycles(config->adcSampleCycles);
        ControlMain::controlHandler = control;
        ControlMain::mainHandler = main;

        ControlMain::startTimers();
        ControlMain::startStopPWMOutput(config->running);
    }
}

namespace ControlMain
{
    bool started = false;
    uint8_t aggregationBuffer[maxMessageSize + 1];
    uint32_t index = 0;

    void handleMessage(uint8_t messageType)
    {
        reset();
        switch ((MessageType)messageType)
        {
        case MessageType::PWMModeConfigMessage:
            PWMMode::handle((PWMModeConfigMessage *)(aggregationBuffer + 1));
            break;
        case MessageType::PidControlConfigMessage:
            PidControlMode::handle((PidControlConfigMessage *)(aggregationBuffer + 1));
            break;
        case MessageType::SimpleControlConfigMessage:
            SimpleControlMode::handle((SimpleControlConfigMessage *)(aggregationBuffer + 1));
            break;

        default:
            break;
        }
        started = true;
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

    uint16_t controlCyclesUsed;

    void onControlLoop()
    {
        controlHandler();
        controlCyclesUsed = TIM9->CNT;
    }

    uint32_t lastStatusSend = 0;
    void controlMain()
    {
        mainHandler();

        auto now = HAL_GetTick();
        if (started && now - lastStatusSend > 500)
        {
            lastStatusSend = now;

            {
                MessageBuffer<SystemStatusMessage> buf(MessageType::SystemStatusMessage);
                readADCValues(1, buf.msg.adcValues);
                buf.msg.controlCpuUsageFraction = controlCyclesUsed / (float)TIM9->ARR;
                while (CDC_Transmit_FS(buf.bytes(), sizeof(buf)) == USBD_BUSY)
                    ;
            }
        }
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