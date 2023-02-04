package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.Circuit;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.github.ruediste.digitalSmpsSim.simulation.ElementOutput;
import com.github.ruediste.digitalSmpsSim.simulation.StepChangingValue;

public class VoltageSource extends CircuitElement {
    public ElementOutput<Voltage> out = new ElementOutput<>(this) {
    };

    public VoltageSource(Circuit circuit) {
        super(circuit);
    }

    public StepChangingValue<Voltage> voltage = new StepChangingValue<>();

    @Override
    public void initialize() {
        out.set(voltage.get(Instant.of(0)));
    }

    @Override
    public void run(Instant stepStart, Instant stepEnd, Duration stepDuration) {
        out.set(voltage.get(stepEnd));
    }
}
