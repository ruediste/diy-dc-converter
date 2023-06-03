package com.github.ruediste.digitalSmpsSim.boost;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.google.common.collect.EvictingQueue;

public abstract class ControlBase<TCircuit extends PowerCircuitBase> extends CircuitElement {
    protected final TCircuit circuit;

    /**
     * Used by the {@link com.github.ruediste.digitalSmpsSim.shared.CostCalculator}
     * to track (and penalty)
     * setpoint changes.
     */
    public abstract double setPoint();

    public final HardwareTimer pwmTimer;
    public final HardwareTimer.Channel pwmChannel;
    public final HardwareTimer.Channel adcChannel;
    public final HardwareTimer controlTimer;

    private List<EvictingQueue<Double>> adcQueues;
    private long adcIteration;

    public double measuredVoltage;

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
                        () -> {
                            if ((adcIteration % adcQueues.size()) == 0) {
                                double voltage = circuit.outputVoltage.get();
                                adcQueues.get(0).add(voltage);
                                measuredVoltage = voltage;
                            }
                            if ((adcIteration % adcQueues.size()) == 1) {
                                adcQueues.get(1).add(circuit.inputVoltage.get());
                            }
                            adcIteration++;
                        }));
        pwmTimer.onReload = (instant) -> {
            if (!pwmChannel.getDisableApplied()) {
                circuit.switchOn.set(true);
            }
        };
        circuit.switchOn.initialize(true);

        adcQueues = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            EvictingQueue<Double> queue = EvictingQueue.create(4);
            adcQueues.add(queue);
            while (queue.remainingCapacity() > 0)
                queue.add(0.);
        }

    }

    public abstract double targetValue(double instant);

    public abstract double actualValue();

    /**
     * Describe the parameters of this controller
     */
    public abstract String parameterInfo();

    public abstract double simulationDuration();

    public abstract double eventTime();

    /**
     * Invoked during construction of the circuit, before initialize()
     */
    public abstract void initializeSteadyState();

    public abstract <T extends PowerCircuitBase> Consumer<T> optimize(List<Supplier<T>> circuitSuppliers);

    protected final void fillAdcChannel(int channel, double value) {
        var queue = adcQueues.get(channel);
        for (int i = 0; i < queue.size(); i++) {
            queue.add(value);
        }
    }

    protected final double readAdcChannel(int channel, int samples) {
        return readAdcChannel(channel, samples, x -> x);
    }

    protected final double readAdcChannel(int channel, int samples, Function<Double, Double> map) {
        double sum = 0;
        var values = adcQueues.get(channel).stream().toList();
        for (int i = 1; i <= samples; i++) {
            sum += map.apply(values.get(values.size() - i));
        }
        return sum / samples;
    }

    @Override
    public void initialize() {
        super.initialize();
        measuredVoltage = circuit.outputVoltage.get();
    }
}
