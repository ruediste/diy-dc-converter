package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.ElementOutput;

import uk.me.berndporr.iirj.ChebyshevI;

public class BoostControlPID extends BoostControlBase {

    public ElementOutput<Voltage> outputVoltageFiltered = new ElementOutput<>(this) {
    };
    public ElementOutput<Voltage> errorOut = new ElementOutput<>(this) {
    };

    protected BoostControlPID(BoostCircuit circuit) {
        super(circuit);
    }

    public double kP = 0.002;
    public double kI = 0.0002;
    public double kD = -2e-1;
    public double lowPass = 4;

    double integral;

    ChebyshevI vOutFilter = new ChebyshevI();
    ChebyshevI vOutFilterSlow = new ChebyshevI();

    boolean firstRun = true;

    @Override
    public void initialize() {
        super.initialize();
        integral = duty / kI;
        vOutFilter.lowPass(3, circuit.switchingFrequency, circuit.switchingFrequency / lowPass, 1);
        vOutFilterSlow.lowPass(3, circuit.switchingFrequency, circuit.switchingFrequency / (2 * lowPass), 1);
        outputVoltageFiltered.set(Voltage.of(0));
        errorOut.set(Voltage.of(0));
    }

    @Override
    protected void updateDuty(Instant currentTime) {
        if (firstRun) {
            firstRun = false;
            for (int i = 0; i < 1000; i++)
                vOutFilter.filter(outputVoltage.get().value());
        }

        double outAvg = vOutFilter.filter(outputVoltage.get().value());
        double outAvgSlow = vOutFilterSlow.filter(outputVoltage.get().value());

        double errorP = targetVoltage.value() - outAvg;
        integral += errorP;
        integral = Math.max(-1 / kI, Math.min(1 / kI, integral));
        double diff = outAvg - outAvgSlow;

        duty = errorP * kP + integral * kI + diff * kD;

        duty = Math.max(0.01, Math.min(duty, 0.99));
        outputVoltageFiltered.set(Voltage.of(outAvg));
        errorOut.set(Voltage.of(errorP));
    }

}
