package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;

public class BoostPower extends CircuitElement {

    private PowerCircuitBase circuit;

    protected BoostPower(PowerCircuitBase circuit) {
        super(circuit);
        this.circuit = circuit;
    }

    public double iL = 0;

    public double inductance = 150e-6;
    public double capacitance = 100e-7;

    @Override
    public void initialize() {

    }

    private double inductorVoltage() {
        var vIn = circuit.inputVoltage.get();
        if (circuit.switchOn.get()) {
            return vIn;
        } else {
            return vIn - circuit.outputVoltage.get();
        }
    }

    @Override
    public void run(double stepStart, double stepEnd, double stepDuration) {
        double vL = inductorVoltage();

        // V=L*di/dt; di=V*dt/L
        var dIL = vL * stepDuration / inductance;

        var iLOld = iL;

        // max: simulate diode: never let current flow backwards
        iL = Math.max(iL + dIL, 0);

        double iC;
        if (circuit.switchOn.get())
            iC = 0; // no current flowing into the output when the switch is on
        else
            iC = (iL + iLOld) / 2; // average inductor current

        iC = iC - circuit.outputCurrent.get(); // no matter of the switch position, the load current is always drawn
                                               // from the
        // capacitor

        // I=C*dv/dt; dv=I*dt/C
        double vOut = circuit.outputVoltage.get();
        vOut += iC * stepDuration / capacitance;

        circuit.outputVoltage.set(vOut);
    }

    @Override
    public Double stepEndTime(double stepStart) {
        double vL = inductorVoltage();
        if (vL < -1e-8) {
            // determine when the current reaches zero, to correctly handle DCM
            // V=L*di/dt; dt=L*di/V
            return stepStart - inductance * iL / vL;
        }

        return null;
    }

}
