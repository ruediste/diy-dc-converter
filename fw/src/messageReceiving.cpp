#include "messageReceiving.h"
#include "interface/messages.h"
#include <string.h>
#include "main.h"
#include "USB_DEVICE/App/usbd_cdc_if.h"

namespace PWMMode
{
    void handle(PWMModeConfigMessage *config)
    {
        htim1.Instance->ARR = config->reload;
        htim1.Instance->CCR1 = config->compare;
        htim1.Instance->PSC = config->prescale;

        HAL_TIM_Base_Start(&htim1);
        HAL_TIM_PWM_Start(&htim1, TIM_CHANNEL_1);
        // HAL_TIM_PWM_Stop(&htim1, TIM_CHANNEL_1);
    }
}

namespace messageReceiving
{
    uint8_t aggregationBuffer[maxMessageSize + 1];
    uint32_t index = 0;

    void handleMessage(uint8_t messageType)
    {
        switch ((MessageType)messageType)
        {
        case MessageType::PWMModeConfigMessage:
            PWMMode::handle((PWMModeConfigMessage *)(aggregationBuffer + 1));
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
}

void receiveData(uint8_t *buf, uint32_t *len)
{
    messageReceiving::receiveData(buf, len);
}