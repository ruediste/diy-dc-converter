package com.github.ruediste.digitalSmpsSim.boost;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;

public abstract class ControlBase<TCircuit extends PowerCircuitBase> extends CircuitElement {
    protected final TCircuit circuit;

    public double duty = 0.5;

    public final HardwareTimer pwmTimer;
    public final HardwareTimer.Channel pwmChannel;
    public final HardwareTimer.Channel adcChannel;
    public final HardwareTimer controlTimer;

    double measuredOutputVoltage;

    protected ControlBase(TCircuit circuit) {
        super(circuit);
        this.circuit = circuit;
        pwmTimer = new HardwareTimer(circuit);
        controlTimer = new HardwareTimer(circuit);
        pwmChannel = pwmTimer.createChannel((instant) -> {
            circuit.switchOn.set(false);
        });
        adcChannel = pwmTimer.createChannel((instant) -> circuit
                .withUpdatedValues(
                        () -> measuredOutputVoltage = circuit.outputVoltage.get()));
        pwmTimer.onReload = (instant) -> {
            circuit.switchOn.set(true);
        };
        circuit.switchOn.initialize(true);
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
