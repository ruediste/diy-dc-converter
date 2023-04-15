package com.github.ruediste.digitalSmpsSim.boost;

public class BoostDutyCalculator {
    public double switchingFrequency = 100e3;
    public double inputVoltage = 5;
    public double outputVoltage = 10;
    public double outputCurrent = 2;
    public double inductance = 3e-3;

    public double switchingPeriod() {
        return 1 / switchingFrequency;
    }

    public double inputCurrent() {
        // iIn*vIn=iOut*vOut; iIn=iOut*vOut/vIn
        return outputCurrent * outputVoltage / inputVoltage;
    }

    public static class BoostDesignResult {
        public double duty;
        public double initialInductorCurrent;
    }

    public BoostDesignResult calculate() {
        var result = new BoostDesignResult();
        // Vo= Vin/(1-D); 1-D=Vin/Vo; D-1=-Vin/Vo; D=1-Vin/Vo;
        double ccmDuty = 1 - inputVoltage / outputVoltage;

        // v=L*di/dt; di=v*dt/L;
        double iRippleCcm = inputVoltage * ccmDuty * switchingPeriod() / inductance;

        if (inputCurrent() > iRippleCcm / 2) {
            // ccm mode
            result.duty = ccmDuty;
            result.initialInductorCurrent = inputCurrent() - (iRippleCcm / 2);
        } else {
            // dcm mode
            // v=L*di/dt; dt=L*di/v; di=v*dt/L;

            // iPeak=vIn*dutyTime/L

            // dropTime=L*iPeak/(vOut-vIn)
            // dropTime=L*vIn*dutyTime/(L*(vOut-vIn))
            // dropTime=vIn*dutyTime/(vOut-vIn)
            // dropTime=dutyTime*vIn/(vOut-vIn)
            // k=vIn/(vOut-vIn)
            // dropDuty=duty*k

            // inputCurrent=(duty+dropDuty)*iPeak/2=duty*(k+1)*iPeak/2
            // inputCurrent=duty*(k+1)*vIn*duty*period/(2*L)
            // duty=sqrt(2*L*inputCurrent/((k+1)*vIn*period))
            var k = inputVoltage / (outputVoltage - inputVoltage);
            result.duty = Math.sqrt(2 * inductance * inputCurrent() / ((k + 1) * inputVoltage * switchingPeriod()));
        }
        return result;
    }
}
