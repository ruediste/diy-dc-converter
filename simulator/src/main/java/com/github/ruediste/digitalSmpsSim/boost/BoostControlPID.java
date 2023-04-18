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

    public double kP = 1.016e-3;
    public double kI = 1.438e-4;
    public double kD = 1.369e-3;

    public double switchingFrequency = 7e3;
    public double controlFrequency = 7e3;

    public double lowPass = 4;

    public int integral;

    // ChebyshevI vOutFilter = new ChebyshevI();
    // ChebyshevI vOutFilterSlow = new ChebyshevI();

    boolean firstRun = true;

    @Override
    public void initialize() {
        var calc = new PwmValuesCalculator();
        {
            var values = calc.calculate(switchingFrequency, duty);
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

    int lastError;

    private int voltageToAdc(double voltage) {
        double maxVoltage = 20;
        return (int) (voltage / maxVoltage * 4096);
    }

    private void control(double instant) {

        int adc = voltageToAdc(circuit.outputVoltage.get());

        int error = voltageToAdc(targetVoltage.get(instant)) - adc;
        if (kI != 0) {
            integral += error;
            int maxIntegral = (int) (1 / kI);
            integral = Math.max(-maxIntegral, Math.min(maxIntegral, integral));
        }

        double diff = error - lastError;

        duty = error * kP + integral * kI + diff * kD;

        if (error < 0.05) {
            // integral += error;
        }

        duty = Math.max(0.001, Math.min(duty, 0.99));

        lastError = error;

        pwmChannel.compare = (long) (duty * pwmTimer.reload);
        circuit.duty.set(duty);
    }

    public <T extends PowerCircuitBase> Consumer<T> optimize(List<Supplier<T>> circuitSuppliers) {
        var optimizer = new Optimizer();
        return optimizer.optimize(
                List.of(
                        new Optimizer.OptimizationParameter<BoostControlPID>("kP", Math.log(kD), 1, -15, 10,
                                (c, v) -> c.kP = Math.exp(v)),
                        new Optimizer.OptimizationParameter<BoostControlPID>("kI", Math.log(kD), 1, -15, 10,
                                (c, v) -> c.kI = Math.exp(v)),
                        new Optimizer.OptimizationParameter<BoostControlPID>("kD", Math.log(kD), 1, -15, 10,
                                (c, v) -> c.kD = Math.exp(v))

                ),
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
        return String.format("kP: %.3e kI: %.3e kD: %.3e", kP, kI, kD);
    }

    @Override
    public double simulationDuration() {
        return 500 / controlFrequency;
    }

    @Override
    public double eventTime() {
        return 250 / controlFrequency;
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
        circuit.duty.initialize(duty);
        if (kI != 0)
            integral = (int) (duty / kI);
        circuit.power.iL = result.initialInductorCurrent;
    }
}
