package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.github.ruediste.digitalSmpsSim.simulation.StepChangingValue;

public class VoltageSource extends CircuitElement {
    private PowerCircuitBase circuit;

    public VoltageSource(PowerCircuitBase circuit) {
        super(circuit);
        this.circuit = circuit;
    }

    public StepChangingValue<Double> voltage = new StepChangingValue<>();

    @Override
    public void initialize() {
        circuit.inputVoltage.set(voltage.get(0));
    }

    @Override
    public void run(double stepStart, double stepEnd, double stepDuration) {
        circuit.inputVoltage.set(voltage.get(stepEnd));
    }
}
