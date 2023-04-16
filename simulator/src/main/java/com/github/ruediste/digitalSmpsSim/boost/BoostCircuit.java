package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;

public class BoostCircuit extends PowerCircuitBase {

    public BoostPower power = new BoostPower(this);

    @Override
    public void initialize() {
        super.initialize();
    }
}
