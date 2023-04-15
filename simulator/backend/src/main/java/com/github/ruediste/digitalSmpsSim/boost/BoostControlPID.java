package com.github.ruediste.digitalSmpsSim.boost;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.ruediste.digitalSmpsSim.optimization.Optimizer;
import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;
import com.github.ruediste.digitalSmpsSim.shared.PwmValuesCalculator;
import com.github.ruediste.digitalSmpsSim.simulation.StepChangingValue;

public class BoostControlPID extends ControlBase<BoostCircuit> {

    public BoostControlPID(BoostCircuit circuit) {
        super(circuit);
    }

    public StepChangingValue<Double> targetVoltage = new StepChangingValue<>();

    public double kP = 0.136;
    public double kI = 0.0049;
    public double kD = 3.5;

    public double switchingFrequency = 100e3;
    public double controlFrequency = 10e3;

    public double lowPass = 4;

    double integral;

    // ChebyshevI vOutFilter = new ChebyshevI();
    // ChebyshevI vOutFilterSlow = new ChebyshevI();

    boolean firstRun = true;

    @Override
    public void initialize() {
        super.initialize();

        var calc = new PwmValuesCalculator();
        {
            var values = calc.calculate(switchingFrequency, 0.1);
            pwmTimer.prescale = values.prescale;
            pwmTimer.reload = values.reload;
            pwmChannel.compare = values.compare;
            adcChannel.compare = 0;
        }
        {
            var values = calc.calculate(controlFrequency, 0.1);
            controlTimer.prescale = values.prescale;
            controlTimer.reload = values.reload;
        }

        // vOutFilter.lowPass(3, circuit.switchingFrequency, circuit.switchingFrequency
        // / lowPass, 1);
        // vOutFilterSlow.lowPass(3, circuit.switchingFrequency,
        // circuit.switchingFrequency / (2 * lowPass), 1);

        controlTimer.onReload = this::control;
    }

    double lastOutputVoltage;

    private void control(double instant) {

        double outAvg = circuit.outputVoltage.get();
        double outAvgSlow = lastOutputVoltage;

        double errorP = outAvg / targetVoltage.get(instant) - 1;
        if (kI != 0) {
            integral += errorP;
            integral = Math.max(-1 / kI, Math.min(1 / kI, integral));
        }

        double diff = (outAvg - outAvgSlow) / targetVoltage.get(instant);

        duty = -(errorP * kP + integral * kI + diff * kD);

        duty = Math.max(0.01, Math.min(duty, 0.99));

        pwmChannel.compare = (long) duty * pwmTimer.reload;

        lastOutputVoltage = circuit.outputVoltage.get();
    }

    public <T extends PowerCircuitBase> Consumer<T> optimize(List<Supplier<T>> circuitSuppliers) {
        var optimizer = new Optimizer();
        return optimizer.optimize(
                List.of(
                        new Optimizer.OptimizationParameter<BoostControlPID>("kP", Math.log(kD), 5, -10, 10,
                                (c, v) -> c.kP = Math.exp(v)),
                        new Optimizer.OptimizationParameter<BoostControlPID>("kI", Math.log(kD), 5, -10, 10,
                                (c, v) -> c.kI = Math.exp(v)),
                        new Optimizer.OptimizationParameter<BoostControlPID>("kD", Math.log(kD), 5, -10, 10,
                                (c, v) -> c.kD = Math.exp(v))),
                circuitSuppliers);
    }

    @Override
    public double targetValue(double instant) {
        return targetVoltage.get(instant);
    }

    @Override
    public double actualValue() {
        return circuit.outputVoltage.get();
    }

    @Override
    public String parameterInfo() {
        return String.format("kP: %f kI: %f kD: %f", kP, kI, kD);
    }

    @Override
    public double simulationDuration() {
        return 200 / controlFrequency;
    }

    @Override
    public double eventTime() {
        return 5 / controlFrequency;
    }

    @Override
    public void initializeSteadyState() {
        var calc = new BoostDutyCalculator();
        calc.inductance = circuit.power.inductance;
        calc.inputVoltage = circuit.source.voltage.get(0);
        calc.outputVoltage = targetVoltage.get(0);
        calc.outputCurrent = circuit.load.calculateCurrent(calc.outputVoltage, 0);
        calc.switchingFrequency = switchingFrequency;
        var result = calc.calculate();
        duty = result.duty;
        integral = -duty / kI;
    }
}
