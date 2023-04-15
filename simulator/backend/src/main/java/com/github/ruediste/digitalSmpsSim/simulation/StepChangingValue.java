package com.github.ruediste.digitalSmpsSim.simulation;

import java.util.TreeMap;

public class StepChangingValue<T> {

    private TreeMap<Double, T> values = new TreeMap<>();

    public void set(double time, T value) {
        values.put(time, value);
    }

    public T get(double time) {
        return values.floorEntry(time).getValue();
    }
}
