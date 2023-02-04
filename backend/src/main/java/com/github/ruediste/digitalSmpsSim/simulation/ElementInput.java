package com.github.ruediste.digitalSmpsSim.simulation;

import com.github.ruediste.digitalSmpsSim.quantity.Quantity;

abstract public class ElementInput<T extends Quantity> implements Plottable {
    private ElementOutput<? extends T> output;

    private T value;

    public T get() {
        return value;
    }

    public ElementInput(CircuitElement element) {
        element.inputs.add(this);
    }

    public void connect(ElementOutput<? extends T> output) {
        this.output = output;
    }

    public void transferValue() {
        value = output.value;
    }

    @Override
    public double plotValue() {
        return value.plotValue();
    }
}
