package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.simulation.Circuit;

public class PowerCircuitBase extends Circuit {

    public VoltageSource source = new VoltageSource(this);

    public Load load = new Load(this);
}
