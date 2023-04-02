package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.quantity.DigitalValue;
import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Fraction;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.github.ruediste.digitalSmpsSim.simulation.StepChangingValue;

public abstract class BoostControlBase extends CircuitElement {
    protected final BoostCircuit circuit;

    public double duty = 0.5;

    public StepChangingValue<Voltage> targetVoltage = new StepChangingValue<>();
    public HardwareTimer pwmTimer = new HardwareTimer(this);
    public HardwareTimer.Channel pwmChannel = pwmTimer.createChannel(null);
    public HardwareTimer.Channel adcChannel = pwmTimer.createChannel(null);
    public HardwareTimer controlTimer = new HardwareTimer(this);

    double outputVoltageAdc;

    protected BoostControlBase(BoostCircuit circuit) {
        super(circuit);
        this.circuit = circuit;
        pwmTimer.onOverflow = () -> circuit.switchOn.set(true);
        pwmChannel.onCompare = () -> circuit.switchOn.set(false);
        adcChannel.onCompare = () -> circuit.withUpdatedValues(() -> outputVoltageAdc = circuit.outputVoltage.get());
    }
}
