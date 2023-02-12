package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.quantity.Instant;

public class BoostControlStepUpDown extends BoostControlBase {

    protected BoostControlStepUpDown(BoostCircuit circuit) {
        super(circuit);
    }

    @Override
    protected void updateDuty(Instant currentTime) {
        if (count % 1 == 0) {
            if (outputVoltage.get().value() < targetVoltage.value()) {
                duty += 1e-2;
            } else
                duty -= 1e-2;
            duty = Math.max(0.01, Math.min(duty, 0.99));
        }
    }

}
