package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.Circuit;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.github.ruediste.digitalSmpsSim.simulation.ElementOutput;

public class VoltageSource extends CircuitElement {
    public ElementOutput<Voltage> out = new ElementOutput<>(this);

    public VoltageSource(Circuit circuit) {
        super(circuit);
    }

    public Voltage voltage;

    @Override
    public void initialize() {
        out.set(voltage);
    }

}
