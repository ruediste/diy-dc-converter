package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.github.ruediste.digitalSmpsSim.simulation.StepChangingValue;

public class Load extends CircuitElement {

    private PowerCircuitBase circuit;

    protected Load(PowerCircuitBase circuit) {
        super(circuit);
        this.circuit = circuit;
    }

    public StepChangingValue<Double> resistance = new StepChangingValue<>();

    @Override
    public void initialize() {
        circuit.loadCurrent.set(0.);
    }

    @Override
    public void run(double stepStart, double stepEnd, double stepDuration) {
        circuit.loadCurrent.set(calculateCurrent(circuit.outputVoltage.get(), stepEnd));
    }

    public double calculateCurrent(double voltage, double instant) {
        return voltage / resistance.get(instant);
    }

}
