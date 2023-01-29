package com.github.ruediste.digitalSmpsSim.simulation;

import java.util.ArrayList;
import java.util.List;

public class Circuit {

    public List<CircuitElement> elements = new ArrayList<>();

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
