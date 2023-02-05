package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.quantity.DigitalValue;
import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Fraction;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.github.ruediste.digitalSmpsSim.simulation.ElementInput;
import com.github.ruediste.digitalSmpsSim.simulation.ElementOutput;

public class BoostControl extends CircuitElement {

    public ElementOutput<DigitalValue> switchOut = new ElementOutput<>(this) {
    };
    public ElementOutput<Fraction> dutyOut = new ElementOutput<>(this) {
    };

    public ElementInput<Voltage> outputVoltage = new ElementInput<>(this) {
    };

    private BoostCircuit circuit;

    public double duty = 0.5;

    public Voltage targetVoltage;

    protected BoostControl(BoostCircuit circuit) {
        super(circuit);
        this.circuit = circuit;
    }

    @Override
    public void initialize() {
        phase = Phase.SWITCH_ON;
        nextPhaseChange = Instant.of(circuit.switchingPeriod() * duty);
        lastCycleStart = Instant.of(0);
        switchOut.set(DigitalValue.HIGH);
        dutyOut.set(Fraction.of(duty));
    }

    private enum Phase {
        SWITCH_ON,
        SWITCH_OFF,
    }

    Phase phase;
    Instant lastCycleStart;
    Instant nextPhaseChange;
    long count;

    @Override
    public Instant stepEndTime(Instant stepStart) {
        moveToTime(stepStart);
        return nextPhaseChange;
    }

    @Override
    public void run(Instant stepStart, Instant stepEnd, Duration stepDuration) {
        moveToTime(stepEnd);
        switchOut.set(DigitalValue.of(phase == Phase.SWITCH_ON));
        dutyOut.set(Fraction.of(duty));
    }

    private void moveToTime(Instant currentTime) {
        if (currentTime.compareTo(nextPhaseChange) >= 0) {
            switch (phase) {
                case SWITCH_ON: {
                    phase = Phase.SWITCH_OFF;
                    nextPhaseChange = lastCycleStart.add(circuit.switchingPeriod());
                }
                    break;
                case SWITCH_OFF: {
                    // we crossed the end of a cycle
                    lastCycleStart = lastCycleStart.add(circuit.switchingPeriod());
                    count++;
                    phase = Phase.SWITCH_ON;

                    if (count % 5 == 0) {
                        if (outputVoltage.get().value() < targetVoltage.value()) {
                            duty += 1e-2;
                        } else
                            duty -= 1e-2;
                        duty = Math.max(0.01, Math.min(duty, 0.99));
                    }

                    nextPhaseChange = lastCycleStart.add(circuit.switchingPeriod() * duty);

                }
                    break;

            }
        }
    }
}
