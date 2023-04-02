package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.simulation.Circuit;
import com.github.ruediste.digitalSmpsSim.simulation.SimulationValue;

public class PowerCircuitBase extends Circuit {

    public VoltageSource source = new VoltageSource(this);

    public Load load = new Load(this);

    public SimulationValue<Boolean> switchOn = new SimulationValue<>(this, false);
    public SimulationValue<Double> inputVoltage = new SimulationValue<>(this, 0.);
    public SimulationValue<Double> loadCurrent = new SimulationValue<>(this, 0.);
    public SimulationValue<Double> outputVoltage = new SimulationValue<>(this, 0.);
    public SimulationValue<Double> outputCurrent = new SimulationValue<>(this, 0.);

}
