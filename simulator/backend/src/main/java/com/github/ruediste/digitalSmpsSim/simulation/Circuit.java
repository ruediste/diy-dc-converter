package com.github.ruediste.digitalSmpsSim.simulation;

import java.util.ArrayList;
import java.util.List;

import com.github.ruediste.digitalSmpsSim.shared.CostCalculator;

public class Circuit {
    public List<Plot> plots = new ArrayList<>();

    public List<CircuitElement> elements = new ArrayList<>();

    public CostCalculator costCalculator = new CostCalculator(this);

    protected void register(CircuitElement element) {
        this.elements.add(element);
    }

    public void propagateSignals() {
        for (var element : elements) {
            for (var input : element.inputs) {
                input.transferValue();
            }
        }
    }

    public void initialize() {
    }
}
