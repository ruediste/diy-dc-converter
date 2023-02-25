package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.quantity.Current;
import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Resistance;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.Circuit;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.github.ruediste.digitalSmpsSim.simulation.ElementInput;
import com.github.ruediste.digitalSmpsSim.simulation.ElementOutput;
import com.github.ruediste.digitalSmpsSim.simulation.StepChangingValue;

public class Load extends CircuitElement {

    public ElementInput<Voltage> voltage = new ElementInput<>(this) {
    };
    public ElementOutput<Current> current = new ElementOutput<>(this) {
    };

    protected Load(Circuit circuit) {
        super(circuit);
    }

    public StepChangingValue<Resistance> resistance = new StepChangingValue<>();

    @Override
    public void initialize() {
        current.set(Current.of(0));
    }

    @Override
    public void run(Instant stepStart, Instant stepEnd, Duration stepDuration) {
        current.set(voltage.get().divide(resistance.get(stepEnd)));
    }

}
