package com.github.ruediste.digitalSmpsSim.simulation;

public abstract class CircuitElement {

    public Circuit circuit;

    protected CircuitElement(Circuit circuit) {
        this.circuit = circuit;
        circuit.register(this);
    }

    public void initialize() {
    }

    public void postInitialize() {
    }

    public void finish() {
    }

    public Double stepEndTime(double stepStart) {
        return null;
    }

    public void run(double stepStart, double stepEnd, double stepDuration) {

    }
}
