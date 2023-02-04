package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.quantity.DigitalValue;
import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Fraction;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.github.ruediste.digitalSmpsSim.simulation.ElementInput;
import com.github.ruediste.digitalSmpsSim.simulation.ElementOutput;

public class BoostControl extends CircuitElement {

    public ElementOutput<DigitalValue> switchOut = new ElementOutput<>(this) {
    };
    public ElementOutput<Fraction> dutyOut = new ElementOutput<>(this) {
    };

    public ElementInput<Voltage> outputVoltage = new ElementInput<>(this) {
    };

    private BoostCircuit circuit;

    public double duty = 0.5;

    protected BoostControl(BoostCircuit circuit) {
        super(circuit);
        this.circuit = circuit;
    }

    private double stepPosition(Instant instant) {
        var tmp = instant.value() / circuit.switchingPeriod();
        return tmp - Math.floor(tmp);
    }

    @Override
    public void initialize() {
        switchOut.set(DigitalValue.HIGH);
    }

    @Override
    public void run(Instant stepStart, Instant stepEnd, Duration stepDuration) {
        var pos = stepPosition(stepEnd) + 1e-10; // move the position to the past to avoid rounding effects
        switchOut.set(DigitalValue.of((pos < duty) || pos > 1));
    }

    @Override
    public Instant stepEndTime(Instant stepStart) {
        var pos = stepPosition(stepStart);

        var posAdjusted = pos + 1e-10;
        if (posAdjusted < duty) {
            return stepStart.add((duty - pos) * circuit.switchingPeriod());
        }

        if (posAdjusted < 1)

            return stepStart.add((1 - pos) * circuit.switchingPeriod());
        return stepStart.add((duty - (pos - 1)) * circuit.switchingPeriod());
    }

}
