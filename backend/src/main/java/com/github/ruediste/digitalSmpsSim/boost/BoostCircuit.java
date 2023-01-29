package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;

public class BoostCircuit extends PowerCircuitBase {

    public BoostPower power = new BoostPower(this);
    public BoostControl control = new BoostControl(this);

    {
        power.vIn.connect(source.out);
        power.switchIn.connect(control.switchOut);
        power.iLoad.connect(load.current);

        load.voltage.connect(power.vOut);

        control.outputVoltage.connect(power.vOut);
    }

    public double switchingFrequency = 100e3;

    public double switchingPeriod() {
        return 1 / switchingFrequency;
    }

    @Override
    public void initialize() {
        super.initialize();
    }
}
