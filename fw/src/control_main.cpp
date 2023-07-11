#include "control_main.h"
#include "interface/messages.h"
#include <string.h>
#include "main.h"
#include "USB_DEVICE/App/usbd_cdc_if.h"
#include <algorithm>
#include <math.h>

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

typedef union
{
    float from;
    uint32_t to;
} FloatUIntUnion;

uint32_t floatToBits(float value)
{
    FloatUIntUnion un;
    un.from = value;
    return un.to;
}

struct ExponentialMovingStatistic
{
    double alpha;

    double average;
    double variance;

    ExponentialMovingStatistic(double averageAge, double samplePeriod)
    {
        alpha = 1 / (averageAge / samplePeriod + 1);
    }

    void add(double value)
    {
        average = alpha * value + (1 - alpha) * average;
        double error = value - average;
        variance = alpha * error * error + (1 - alpha) * variance;
    }
};

struct PwmValuesCalculator
{

    uint16_t reload;
    uint16_t compare;
    uint16_t prescale;

    PwmValuesCalculator(double frequency, double duty)
    {
        prescale = ((long)ceil(clock / ((1 << bits) * frequency))) - 1;
        reload = round(clock / ((prescale + 1) * frequency));
        compare = round(reload * duty);
    }

private:
    const double clock = 84e6;
    const long bits = 16;
};

namespace ControlMain
{

    void
    dummyControlHandler()
    {
    }
    void dummyMainHandler()
    {
    }

    void (*controlHandler)() = dummyControlHandler;
    void (*mainHandler)() = dummyMainHandler;

    uint blobMessageIndex;
    volatile bool blobTriggered = false;
    volatile bool blobFull = false;
    MessageBuffer<BlobMessage> blobMessage(MessageType::BlobMessage);

    void reset()
    {
        blobTriggered = false;
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

    void control()
    {
        if (ControlMain::blobTriggered && !ControlMain::blobFull)
        {
            uint16_t adcValues[ControlMain::adcChannelCount];
            ControlMain::readADCValues(1, adcValues);
            auto adc = adcValues[0];

            uint &idx = ControlMain::blobMessageIndex;
            if (idx + 2 <= sizeof(ControlMain::blobMessage.msg))
            {
                uint8_t *data = ControlMain::blobMessage.msg.data;
                data[idx++] = adc;
                data[idx++] = adc >> 8;
            }
            else
            {
                ControlMain::blobFull = true;
            }
        }
    }

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

        TIM9->PSC = config->ctrlPrescale;
        TIM9->ARR = config->ctrlReload;

        ControlMain::mainHandler = main;
        ControlMain::controlHandler = control;

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
    int32_t integral;
    int32_t lastError;

    uint32_t lastStatusSend = 0;

    void control()
    {
        uint16_t adcValues[ControlMain::adcChannelCount];
        ControlMain::readADCValues(1, adcValues);

        auto adc = adcValues[0];
        int32_t error = config.targetAdc - adc;

        integral += error;
        integral = std::max(-config.maxIntegral, std::min(config.maxIntegral, integral));

        float diff = error - lastError;

        duty = error * config.kP + integral * config.kI + diff * config.kD;

        duty = std::max(0.0f, std::min(duty, config.maxDuty));
        lastError = error;

        TIM1->CCR1 = duty * TIM1->ARR;

        if (ControlMain::blobTriggered && !ControlMain::blobFull)
        {
            uint &idx = ControlMain::blobMessageIndex;
            if (idx + 6 <= sizeof(ControlMain::blobMessage.msg))
            {
                uint8_t *data = ControlMain::blobMessage.msg.data;
                data[idx++] = adc;
                data[idx++] = adc >> 8;
                data[idx++] = TIM1->CCR1;
                data[idx++] = TIM1->CCR1 >> 8;
                data[idx++] = TIM1->CCR1 >> 16;
                data[idx++] = TIM1->CCR1 >> 24;
            }
            else
            {
                ControlMain::blobFull = true;
            }
        }
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
        lastError = 0;
        integral = 0;

        ControlMain::setAdcSampleCycles(config->adcSampleCycles);
        ControlMain::controlHandler = control;
        ControlMain::mainHandler = main;

        ControlMain::startTimers();
        ControlMain::startStopPWMOutput(config->running);
    }

}

namespace CotControlMode
{

    CotControlConfigMessage config;
    enum class Mode
    {
        COT,
        CYCLE_SKIPPING
    };

    float duty = 0;
    float integral;
    int32_t lastError;
    float cotLimitTime;
    Mode mode;
    float pwmEnabledTime;
    float underFrequencyCycles;
    float vOut;
    uint16_t vOutAdc;
    float vIn;
    float frequency;
    uint16_t targetVoltageAdc;

    uint32_t lastStatusSend = 0;
    ExponentialMovingStatistic vOutAdcStats(4, 1);

    float adcToVoltage(uint16_t adcValue)
    {
        return config.maxAdcVoltage * (adcValue / 4096.f);
    }

    uint16_t voltageToAdc(float voltage)
    {
        return (int)(voltage / config.maxAdcVoltage * 4096.f);
    }

    float calculateCurrent(float fallTime, float period)
    {
        return config.peakCurrent * fallTime / (2 * period);
    }

    void enableDisablePWM(bool enable)
    {
        TIM1->CCMR1 = (TIM1->CCMR1 & ~TIM_CCMR1_OC1M_Msk) | ((enable ? 0b110 : 0b100) << TIM_CCMR1_OC1M_Pos);
    }

    float onTime;
    float fallTime;
    void control()
    {
        uint16_t adcValues[ControlMain::adcChannelCount];
        ControlMain::readADCValues(1, adcValues);

        vOutAdc = adcValues[0];
        vOutAdcStats.add(vOutAdc);
        vOut = adcToVoltage(vOutAdc);

        // uint16_t vInAdc = adcValues[1];
        // vIn = adcToVoltage(vInAdc);
        vIn = 5;

        // v=L*di/dt; t=L*iPeak/vIn
        onTime = config.inductance * config.peakCurrent / vIn;
        fallTime = config.inductance * config.peakCurrent / (std::max(vOut, vIn * config.startupVoltageFactor) - vIn);
        float minTime = (onTime + fallTime) / (1 - config.idleFraction);
        float iOutMax = calculateCurrent(fallTime, minTime);

        float iCotLimit = calculateCurrent(fallTime, cotLimitTime);

        int32_t error = (targetVoltageAdc - vOutAdc);

        if (cotLimitTime < minTime)
        {
            mode = Mode::CYCLE_SKIPPING;
            pwmEnabledTime = 0;
        }

        switch (mode)
        {
        case Mode::COT:
        {
            // limit integral
            integral = std::max(std::min(integral, iOutMax), iCotLimit);

            integral += config.kI * error;
            float diff = error - lastError;

            double outputCurrent = error * config.kP + integral + diff * config.kD;

            // limit output current
            if (outputCurrent > iOutMax)
            {
                // we are in over current mode
                outputCurrent = iOutMax;
            }
            if (outputCurrent < iCotLimit)
            {
                // we are below the switching current
                underFrequencyCycles++;
                outputCurrent = iCotLimit;
                enableDisablePWM(false);
            }
            else
            {
                underFrequencyCycles = 0;
                enableDisablePWM(true);
            }

            double cycleTime = config.peakCurrent * fallTime / (2 * outputCurrent);
            frequency = 1 / cycleTime;
            if (underFrequencyCycles > 5)
            {
                mode = Mode::CYCLE_SKIPPING;
                enableDisablePWM(false);
                pwmEnabledTime = 0;
            }
            else
            {
                PwmValuesCalculator calc(frequency, onTime / cycleTime);
                TIM1->ARR = calc.reload;
                TIM1->PSC = calc.prescale;
                TIM1->CCR1 = calc.compare;
            }
        }
        break;
        case Mode::CYCLE_SKIPPING:
        {
            double period = std::max(minTime, cotLimitTime / 2);
            frequency = 1 / period;

            PwmValuesCalculator calc(frequency, onTime / period);
            TIM1->ARR = calc.reload;
            TIM1->PSC = calc.prescale;
            TIM1->CCR1 = calc.compare;

            if (vOutAdc > targetVoltageAdc)
            {
                enableDisablePWM(false);
                pwmEnabledTime = 0;
            }
            else
            {
                enableDisablePWM(true);
                pwmEnabledTime += (1 / config.controlFrequency) / period;
                if (pwmEnabledTime > 6 && error * error > 2 * vOutAdcStats.variance)
                {
                    mode = Mode::COT;
                    integral = calculateCurrent(fallTime, period);
                    lastError = error;
                    underFrequencyCycles = 0;
                }
            }
        }
        break;
        }

        if (ControlMain::blobTriggered && !ControlMain::blobFull)
        {
            uint &idx = ControlMain::blobMessageIndex;
            if (idx + 6 <= sizeof(ControlMain::blobMessage.msg))
            {
                uint8_t *data = ControlMain::blobMessage.msg.data;
                // data[idx++] = adc;
                // data[idx++] = adc >> 8;
                data[idx++] = TIM1->CCR1;
                data[idx++] = TIM1->CCR1 >> 8;
                data[idx++] = TIM1->CCR1 >> 16;
                data[idx++] = TIM1->CCR1 >> 24;
            }
            else
            {
                ControlMain::blobFull = true;
            }
        }

        lastError = error;
    }

    void main()
    {
        auto now = HAL_GetTick();
        if (now - lastStatusSend > 500)
        {
            lastStatusSend = now;

            {
                MessageBuffer<CotControlStatusMessage> buf(MessageType::CotControlStatusMessage);

                // uint16_t adcValues[ControlMain::adcChannelCount];
                // ControlMain::readADCValues(2, adcValues);
                buf.msg.isInHystericMode = mode == Mode::CYCLE_SKIPPING;
                buf.msg.integral = integral;
                buf.msg.vOut = vOut;
                while (CDC_Transmit_FS(buf.bytes(), sizeof(buf)) == USBD_BUSY)
                    ;
            }
            {
                MessageBuffer<DebugMessage> buf(MessageType::DebugMessage);
                uint32_t *buf2 = (uint32_t *)buf.msg.data;
                buf2[0] = vOutAdc;
                buf2[1] = targetVoltageAdc;
                buf2[2] = floatToBits(pwmEnabledTime);
                buf2[3] = TIM1->CCMR1;
                while (CDC_Transmit_FS(buf.bytes(), sizeof(buf)) == USBD_BUSY)
                    ;
                ADC1->SR = 0;
                TIM1->SR = 0;
                // ADC1->CR2 |= ADC_CR2_JSWSTART;
            }
        }
    }

    void handle(CotControlConfigMessage *config)
    {

        CotControlMode::config = *config;
        lastError = 0;
        integral = 0;
        cotLimitTime = 1 / (config->controlFrequency / 2);
        PwmValuesCalculator ctrlValues(config->controlFrequency, 0);
        mode = Mode::COT;
        underFrequencyCycles = 0;
        targetVoltageAdc = voltageToAdc(config->targetVoltage);

        TIM1->ARR = ctrlValues.reload;
        TIM1->PSC = ctrlValues.prescale;
        TIM1->CCR1 = 0;
        TIM1->CCR3 = 10;

        TIM9->PSC = ctrlValues.prescale;
        TIM9->ARR = ctrlValues.reload;

        ControlMain::setAdcSampleCycles(3); // 56 cycles
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

    void handleMessage(MessageType messageType)
    {
        if (messageType == MessageType::TriggerBlobMessage)
        {
            blobMessageIndex = 0;
            blobFull = false;
            blobTriggered = true;
            return;
        }
        reset();
        switch (messageType)
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
        case MessageType::CotControlConfigMessage:
            CotControlMode::handle((CotControlConfigMessage *)(aggregationBuffer + 1));
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
            while (index > 0)
            {
                uint8_t messageType = aggregationBuffer[0];

                uint32_t messageSize = messageSizes[messageType];
                if (index >= messageSize + 1)
                {
                    // complete message is in aggregation buffer
                    handleMessage((MessageType)messageType);

                    // shrink aggregation buffer
                    if (index > messageSize + 1)
                        memmove(aggregationBuffer, aggregationBuffer + index, index - (messageSize + 1));
                    index -= messageSize + 1;
                }
                else
                {
                    break;
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

        if (blobTriggered && blobFull)
        {
            while (CDC_Transmit_FS(blobMessage.bytes(), sizeof(blobMessage)) == USBD_BUSY)
                ;
            blobTriggered = false;
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