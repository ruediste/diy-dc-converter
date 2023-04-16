package com.github.ruediste.digitalSmpsSim.boost;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.ruediste.digitalSmpsSim.optimization.Optimizer;
import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;
import com.github.ruediste.digitalSmpsSim.shared.PwmValuesCalculator;
import com.github.ruediste.digitalSmpsSim.simulation.StepChangingValue;

public class BoostControlStepUpDown extends ControlBase<BoostCircuit> {

    public StepChangingValue<Double> targetVoltage = new StepChangingValue<>();
    public double dutyChangeStep = 0.01;
    public double maxDuty = 0.6;
    public double switchingFrequency = 100e3;
    public double controlFrequency = 10e3;

    protected BoostControlStepUpDown(BoostCircuit circuit) {
        super(circuit);
    }

    @Override
    public void initialize() {
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

        controlTimer.onReload = (instant) -> {
            if (measuredOutputVoltage < targetVoltage.get(instant))
                duty += dutyChangeStep;
            else
                duty -= dutyChangeStep;
            if (duty < 0) {
                duty = 0;
            }
            if (duty > maxDuty) {
                duty = maxDuty;
            }
            pwmChannel.compare = (long) duty * pwmTimer.reload;
            circuit.duty.set(duty);
        };
    }

    public void initializeSteadyState() {
        var calc = new BoostDutyCalculator();
        calc.inductance = circuit.power.inductance;
        calc.inputVoltage = circuit.source.voltage.get(0);
        calc.outputVoltage = targetVoltage.get(0);
        calc.outputCurrent = circuit.load.calculateCurrent(calc.outputVoltage, 0);
        calc.switchingFrequency = switchingFrequency;
        var result = calc.calculate();
        duty = result.duty;
    }

    public double eventTime() {
        return 5 / switchingFrequency;
    }

    public double simulationDuration() {
        return 200 / switchingFrequency;
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
        return "Step: " + dutyChangeStep;
    }

    @Override
    public <T extends PowerCircuitBase> Consumer<T> optimize(List<Supplier<T>> circuitSuppliers) {
        var optimizer = new Optimizer();
        return optimizer.optimize(
                List.of(
                        new Optimizer.OptimizationParameter<BoostControlStepUpDown>("step", Math.log(dutyChangeStep), 5,
                                -10, 10,
                                (c, v) -> c.dutyChangeStep = Math.exp(v))),
                circuitSuppliers);
    }

}
