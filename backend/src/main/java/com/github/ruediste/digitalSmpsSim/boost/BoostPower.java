package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.quantity.Current;
import com.github.ruediste.digitalSmpsSim.quantity.DigitalValue;
import com.github.ruediste.digitalSmpsSim.quantity.*;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.github.ruediste.digitalSmpsSim.simulation.ElementInput;
import com.github.ruediste.digitalSmpsSim.simulation.ElementOutput;

public class BoostPower extends CircuitElement {

    public ElementInput<Voltage> vIn = new ElementInput<>(this) {
    };
    public ElementInput<DigitalValue> switchIn = new ElementInput<>(this) {
    };
    public ElementInput<Current> iLoad = new ElementInput<>(this) {
    };

    public ElementOutput<Voltage> vOut = new ElementOutput<>(this) {
    };
    public ElementOutput<Current> ilOut = new ElementOutput<>(this) {
    };

    protected BoostPower(BoostCircuit circuit) {
        super(circuit);
    }

    public Current iL = Current.of(0);
    public Voltage vCap = Voltage.of(0);

    public double inductance = 150e-6;
    public double capacitance = 100e-7;

    @Override
    public void initialize() {
        vOut.set(vCap);
        ilOut.set(iL);
    }

    private Voltage inductorVoltage() {
        if (switchIn.value.isHigh()) {
            return vIn.value;
        } else {
            return vIn.value.minus(vCap);
        }
    }

    @Override
    public void run(Instant stepStart, Instant stepEnd, Duration stepDuration) {
        Voltage vL = inductorVoltage();

        // V=L*di/dt; di=V*dt/L
        var dIL = vL.value() * stepDuration.value() / inductance;

        var iLOld = iL;

        // max: simulate diode: never let current flow backwards
        iL = iL.add(dIL).max(0);

        Current iC = iL.add(iLOld).scale(0.5) // average inductor current
                .minus(iLoad.value);

        // I=C*dv/dt; dv=I*dt/C
        vCap = vCap.add(iC.value() * stepDuration.value() / capacitance);

        vOut.set(vCap);
        ilOut.set(iL);
    }

    @Override
    public Instant stepEndTime(Instant stepStart) {
        Voltage vL = inductorVoltage();
        if (vL.value() < -1e-8) {
            // determine when the current reaches zero, to correctly handle DCM
            // V=L*di/dt; dt=L*di/V
            return stepStart.add(-inductance * iL.value() / vL.value());
        }

        return null;
    }

}
