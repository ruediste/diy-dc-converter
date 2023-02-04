package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.quantity.Current;
import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Resistance;
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
        return outputCurrent().value() * switchingPeriod().value() * duty() / outputRipple.value();
    }

    public Current outputCurrent() {
        return Current.of(inputCurrent.value() * inputVoltage.value() / outputVoltage.value());
    }

    public double inductance() {
        // V=L*di/dt; L=V*dt/di;
        return inputVoltage.value() * switchingPeriod().value() * duty() / (inputCurrent.value() * inductorRipple);
    }

    public Resistance loadResistance() {
        return outputVoltage.divide(outputCurrent());
    }

    @Override
    public String toString() {
        return "frequency: %s in: %s out: %s inCurrent: %s".formatted(SiPrefix.format(switchingFrequency, "Hz"),
                inputVoltage, outputVoltage, inputCurrent)
                + " currentRipple: %.3f outRipple: %s\n".formatted(inductorRipple, outputRipple)
                + "swPeriod: %s duty: %.3f C: %s L: %s Rload: %s".formatted(switchingPeriod(), duty(),
                        SiPrefix.format(outputCapacitance(), "F"), SiPrefix.format(inductance(), "H"),
                        loadResistance());
    }

    public void applyTo(BoostCircuit circuit) {
        circuit.power.iL = inputCurrent.scale(1 - inductorRipple / 2);
        circuit.power.vCap = outputVoltage;
        circuit.source.voltage.set(Instant.of(0), inputVoltage);
        circuit.control.duty = duty();
        circuit.power.inductance = inductance();
        circuit.power.capacitance = outputCapacitance();
        circuit.load.resistance = loadResistance();
    }

    public BoostCircuit circuit() {
        var circuit = new BoostCircuit();
        applyTo(circuit);
        return circuit;
    }
}
