package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;

public class BoostCircuit extends PowerCircuitBase {

    public BoostPower power = new BoostPower(this);
    public BoostControlPID control = new BoostControlPID(this);

    public double switchingFrequency = 100e3;

    {
        power.vIn.connect(source.out);
        power.switchIn.connect(control.switchOut);
        power.iLoad.connect(load.current);

        load.voltage.connect(power.vOut);

        control.outputVoltage.connect(power.vOut);
        costCalculator.outputVoltage.connect(power.vOut);
        costCalculator.evaluationPeriod = Duration.of(switchingPeriod());
        costCalculator.inductorCurrent.connect(power.ilOut);

        costCalculator.targetVoltage = control.targetVoltage;
        costCalculator.duty.connect(control.dutyOut);
    }

    public double switchingPeriod() {

        return 1 / switchingFrequency;
    }

    @Override
    public void initialize() {
        super.initialize();
    }
}
