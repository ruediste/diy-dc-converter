package com.github.ruediste.digitalSmpsSim.simulation;

import java.util.TreeMap;

import com.github.ruediste.digitalSmpsSim.quantity.Instant;

public class StepChangingValue<T> {

    private TreeMap<Double, T> values = new TreeMap<>();

    public void set(Instant time, T value) {
        values.put(time.value(), value);
    }

    public T get(Instant time) {
        return values.floorEntry(time.value()).getValue();
    }
}
