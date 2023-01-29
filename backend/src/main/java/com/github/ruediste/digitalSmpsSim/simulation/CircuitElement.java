package com.github.ruediste.digitalSmpsSim.simulation;

import java.util.ArrayList;
import java.util.List;

import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;

public abstract class CircuitElement {

    public List<ElementInput<?>> inputs = new ArrayList<>();

    protected CircuitElement(Circuit circuit) {
        circuit.register(this);
    }

    public void initialize() {
    }

    public Instant stepEndTime(Instant stepStart) {
        return null;
    }

    public void run(Instant stepStart, Instant stepEnd, Duration stepDuration) {

    }
}
