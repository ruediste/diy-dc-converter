package com.github.ruediste.digitalSmpsSim.boost;

import com.github.ruediste.digitalSmpsSim.quantity.Current;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.ElementOutput;

import uk.me.berndporr.iirj.ChebyshevI;

public class BoostControlPID extends BoostControlBase {

    public ElementOutput<Voltage> errorOut = new ElementOutput<>(this) {
    };
    public ElementOutput<Current> di = new ElementOutput<>(this) {
    };

    protected BoostControlPID(BoostCircuit circuit) {
        super(circuit);
    }

    public double kP = 0.136;
    public double kI = 0.0049;
    public double kD = 3.5;

    public double lowPass = 4;

    double integral;

    ChebyshevI vOutFilter = new ChebyshevI();
    ChebyshevI vOutFilterSlow = new ChebyshevI();

    boolean firstRun = true;

    @Override
    public void initialize() {
        super.initialize();

        integral = kI == 0 ? 0 : -duty / kI;
        vOutFilter.lowPass(3, circuit.switchingFrequency, circuit.switchingFrequency / lowPass, 1);
        vOutFilterSlow.lowPass(3, circuit.switchingFrequency, circuit.switchingFrequency / (2 * lowPass), 1);
        errorOut.set(Voltage.of(0));
        di.set(Current.of(0));
    }

    Voltage lastOutputVoltage = Voltage.of(0);

    @Override
    protected void updateDuty(Instant currentTime) {
        if (firstRun) {
            firstRun = false;
            lastOutputVoltage = outputVoltage.get();
            for (int i = 0; i < 1000; i++) {
                vOutFilter.filter(outputVoltage.get().value());
                vOutFilterSlow.filter(outputVoltage.get().value());
            }
        }

        // PID
        {
            double outAvg = vOutFilter.filter(outputVoltage.get().value());
            double outAvgSlow = vOutFilterSlow.filter(outputVoltage.get().value());

            double errorP = outAvg / targetVoltage.get(currentTime).value() - 1;
            if (kI != 0) {
                integral += errorP;
                integral = Math.max(-1 / kI, Math.min(1 / kI, integral));
            }

            double diff = (outAvg - outAvgSlow) / targetVoltage.get(currentTime).value();

            duty = -(errorP * kP + integral * kI + diff * kD);

            errorOut.set(Voltage.of(errorP));
        }

        // voltage change feed forward
        if (false) {
            double voltageChange = outputVoltage.get().value() - lastOutputVoltage.value();

            // average current required to explain the change in output voltage
            double dI = circuit.power.capacitance * voltageChange
                    / circuit.switchingPeriod();
            di.set(Current.of(dI));

            // required duty the average inductor current by the dI (peak current
            // change times two)
            double dt = 2 * circuit.power.inductance * dI / outputVoltage.get().value();

            double dd = dt / circuit.switchingPeriod();

            if ((voltageChange < 0 && outputVoltage.get().value() < targetVoltage.get(currentTime).value())
                    || (voltageChange > 0 && outputVoltage.get().value() > targetVoltage.get(currentTime).value())) {

                duty -= dd;
            }
        }

        duty = Math.max(0.01, Math.min(duty, 0.99));

        lastOutputVoltage = outputVoltage.get();
    }

}
