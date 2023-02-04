package com.github.ruediste.digitalSmpsSim.simulation;

import com.github.ruediste.digitalSmpsSim.quantity.Quantity;

public abstract class ElementOutput<T extends Quantity> implements Plottable {

    T value;

    public ElementOutput(CircuitElement element) {
    }

    public void set(T value) {
        this.value = value;
    }

    @Override
    public double plotValue() {
        return value.plotValue();
    }

}
