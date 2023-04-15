package com.github.ruediste.digitalSmpsSim.simulation;

import java.util.ArrayList;
import java.util.List;

public class Circuit {
    public List<Plot> plots = new ArrayList<>();

    public List<CircuitElement> elements = new ArrayList<>();

    public List<SimulationValue<?>> values = new ArrayList<>();

    public List<Runnable> withUpdatedValues = new ArrayList<>();

    protected void register(CircuitElement element) {
        this.elements.add(element);
    }

    public void propagateSignals() {
        values.forEach(v -> v.transferNextValue());
    }

    public void initialize() {
    }

    public void withUpdatedValues(Runnable run) {
        this.withUpdatedValues.add(run);
    }
}
