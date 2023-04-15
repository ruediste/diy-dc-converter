package com.github.ruediste.digitalSmpsSim.boost;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;

public abstract class ControlBase<TCircuit extends PowerCircuitBase> extends CircuitElement {
    protected final TCircuit circuit;

    public double duty = 0.5;

    public HardwareTimer pwmTimer = new HardwareTimer(this);
    public HardwareTimer.Channel pwmChannel = pwmTimer.createChannel(null);
    public HardwareTimer.Channel adcChannel = pwmTimer.createChannel(null);
    public HardwareTimer controlTimer = new HardwareTimer(this);

    double measuredOutputVoltage;

    protected ControlBase(TCircuit circuit) {
        super(circuit);
        this.circuit = circuit;
        pwmTimer.onReload = (instant) -> circuit.switchOn.set(true);
        pwmChannel.onCompare = (instant) -> circuit.switchOn.set(false);
        adcChannel.onCompare = (instant) -> circuit
                .withUpdatedValues(
                        () -> measuredOutputVoltage = circuit.outputVoltage.get());
    }

    public abstract double targetValue(double instant);

    public abstract double actualValue();

    /**
     * Describe the parameters of this controller
     */
    public abstract String parameterInfo();

    public abstract double simulationDuration();

    public abstract double eventTime();

    public abstract void initializeSteadyState();

    public abstract <T extends PowerCircuitBase> Consumer<T> optimize(List<Supplier<T>> circuitSuppliers);
}
