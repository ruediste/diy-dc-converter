package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.quantity.Current;
import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Resistance;
import com.github.ruediste.digitalSmpsSim.quantity.SiPrefix;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;

public class BoostDesign {
    public double switchingFrequency = 100e3;
    public double inputVoltage = 5;
    public double outputVoltage = 10;
    public double inputCurrent = 2;
    public double inductorRipple = 0.5;
    public double outputRipple = 0.1;

    public double switchingPeriod() {
        return 1 / switchingFrequency;
    }

    public double duty() {
        // Vo= Vin/(1-D); 1-D=Vin/Vo; D-1=-Vin/Vo; D=1-Vin/Vo;
        return 1 - inputVoltage / outputVoltage;
    }

    public double outputCapacitance() {
        // I=C*dv/dt; C=I*dt/dv
        return outputCurrent() * switchingPeriod() * duty() / outputRipple;
    }

    public double outputCurrent() {
        return inputCurrent * inputVoltage / outputVoltage;
    }

    public double inductance() {
        // V=L*di/dt; L=V*dt/di;
        return inputVoltage * switchingPeriod() * duty() / (inputCurrent * inductorRipple);
    }

    public double loadResistance() {
        return outputVoltage / outputCurrent();
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
        circuit.power.iL = inputCurrent * (1 - inductorRipple / 2);
        circuit.outputVoltage.set( outputVoltage)
        circuit.source.voltage.set(Instant.of(0), inputVoltage);
        circuit.control.duty = duty();
        circuit.power.inductance = inductance();
        circuit.power.capacitance = outputCapacitance();
        circuit.load.resistance.set(Instant.of(0), loadResistance());
    }

    public BoostCircuit circuit() {
        var circuit = new BoostCircuit();
        applyTo(circuit);
        return circuit;
    }
}
