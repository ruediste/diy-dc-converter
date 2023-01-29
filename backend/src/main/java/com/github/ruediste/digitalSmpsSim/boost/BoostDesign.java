package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.quantity.Current;
import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.SiPrefix;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;

public class BoostDesign {
    public double switchingFrequency = 100e3;
    public Voltage inputVoltage = Voltage.of(5);
    public Voltage outputVoltage = Voltage.of(10);
    public Current inputCurrent = Current.of(2);
    public double inductorRipple = 0.1;
    public Voltage outputRipple = Voltage.of(0.050);

    public Duration switchingPeriod() {
        return Duration.of(1 / switchingFrequency);
    }

    public double duty() {
        // Vo= Vin/(1-D); 1-D=Vin/Vo; D-1=-Vin/Vo; D=1-Vin/Vo;
        return 1 - inputVoltage.value() / outputVoltage.value();
    }

    public double outputCapacitance() {
        // I=C*dv/dt; C=I*dt/dv
        return inputCurrent.value() * switchingPeriod().value() * duty() / outputRipple.value();
    }

    public Current outputCurrent() {
        return Current.of(inputCurrent.value() * inputVoltage.value() / outputVoltage.value());
    }

    public double inductance() {
        // V=L*di/dt; L=V*dt/di;
        return inputVoltage.value() * switchingPeriod().value() * duty();
    }

    @Override
    public String toString() {
        return "frequency: %s in: %s out: %s inCurrent: %s".formatted(SiPrefix.format(switchingFrequency, "Hz"),
                inputVoltage, outputVoltage, inputCurrent)
                + "currentRipple: %f outRipple: %s\n".formatted(inductorRipple, outputRipple)
                + "swPeriod: %s duty: %f C: %s L: %s".formatted(switchingPeriod(), duty(),
                        SiPrefix.format(outputCapacitance(), "F"), SiPrefix.format(inductance(), "H"));
    }

    public void applyTo(BoostCircuit circuit) {
        circuit.power.iL = inputCurrent;
        circuit.power.vCap = outputVoltage;
        circuit.source.voltage = inputVoltage;
        circuit.control.duty = duty();

        // U=R*I; R=U/I
        circuit.load.resistance = outputVoltage.divide(outputCurrent());
    }
}
