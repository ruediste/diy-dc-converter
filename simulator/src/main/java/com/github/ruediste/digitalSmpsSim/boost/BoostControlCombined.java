package com.github.ruediste.digitalSmpsSim.boost;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.ruediste.digitalSmpsSim.optimization.Optimizer;
import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;
import com.github.ruediste.digitalSmpsSim.shared.PwmValuesCalculator;
import com.github.ruediste.digitalSmpsSim.simulation.StepChangingValue;

public class BoostControlCombined extends ControlBase<BoostCircuit> {

    public BoostControlCombined(BoostCircuit circuit) {
        super(circuit);
    }

    public StepChangingValue<Double> targetVoltage = new StepChangingValue<>();

    public double kP = 5e0;
    public double kI = 5e0;
    public double kD = 3e2;

    public double alphaLast = 0.01;
    public double alphaFactor = 10;

    public double minimumSwitchingFrequency = 1e3;
    public double controlFrequency = 20e3;
    public double peakCurrent = 60e-3;

    public double integral;
    public double frequency;

    private Random random = new Random(0);

    boolean firstRun = true;

    @Override
    public void initialize() {
        super.initialize();
        var calc = new PwmValuesCalculator();
        {
            var values = calc.calculate(controlFrequency, 0.1);
            controlTimer.prescale = values.prescale;
            controlTimer.reload = values.reload;
        }

        controlTimer.onReload = this::control;
        control(0);
    }

    private int voltageToAdc(double voltage) {
        double maxVoltage = 20;
        return (int) (voltage / maxVoltage * 4096);
    }

    private double adcToVoltage(int adc) {
        double maxVoltage = 20;
        return adc / 4096. * maxVoltage;
    }

    public enum Mode {
        COT,
        CYCLE_SKIPPING
    }

    public Mode mode;
    double lastError;
    int pwmEnabledCycles;
    public int error;
    public double diff;
    public double errorS;

    private void control(double instant) {

        int vOut = (int) readAdcChannel(0, 1, x -> random.nextGaussian(voltageToAdc(x), 4.48));
        int vIn = (int) readAdcChannel(1, 1, x -> random.nextGaussian(voltageToAdc(x), 4.48));

        // v=L*di/dt; t=L*iPeak/vIn
        double onTime = circuit.power.inductance * peakCurrent / adcToVoltage(vIn);
        error = (voltageToAdc(targetVoltage.get(instant)) - vOut);

        switch (mode) {
            case COT: {
                errorS = alphaLast * alphaFactor * error + (1 - alphaLast * alphaFactor) * errorS;
                if (kI != 0) {
                    integral += kI * error;
                }

                diff = errorS - lastError;

                frequency = error * kP + integral + diff * kD;
                frequency = Math.min(frequency, 15e3);

                if (frequency < minimumSwitchingFrequency) {
                    mode = Mode.CYCLE_SKIPPING;
                    pwmChannel.disable = true;
                    pwmEnabledCycles = 0;
                } else {
                    double period = 1 / frequency;
                    var calc = new PwmValuesCalculator();

                    var values = calc.calculate(frequency, onTime / period);
                    pwmTimer.prescale = values.prescale;
                    pwmTimer.reload = values.reload;
                    pwmChannel.compare = values.compare;
                }

                lastError = alphaLast * error + (1 - alphaLast) * lastError;
            }
                break;
            case CYCLE_SKIPPING: {
                frequency = minimumSwitchingFrequency * 3;
                double period = 1 / frequency;
                var calc = new PwmValuesCalculator();

                var values = calc.calculate(frequency, onTime / period);
                pwmTimer.prescale = values.prescale;
                pwmTimer.reload = values.reload;
                pwmChannel.compare = values.compare;

                if (vOut > voltageToAdc(targetVoltage.get(instant))) {
                    pwmChannel.disable = true;
                    pwmEnabledCycles = 0;
                } else {
                    pwmChannel.disable = false;
                    pwmEnabledCycles++;
                    if (pwmEnabledCycles > 50) {
                        mode = Mode.COT;
                        integral = frequency;
                        errorS = error;
                        lastError = error;
                    }
                }

            }
                break;
        }
    }

    public <T extends PowerCircuitBase> Consumer<T> optimize(List<Supplier<T>> circuitSuppliers) {
        var optimizer = new Optimizer();
        return optimizer.optimize(
                List.of(
                        new Optimizer.OptimizationParameter<BoostControlCombined>("kP", Math.log(kD), 2, -15, 10,
                                (c, v) -> c.kP = Math.exp(v)),
                        new Optimizer.OptimizationParameter<BoostControlCombined>("kI", Math.log(kD), 2, -15, 10,
                                (c, v) -> c.kI = Math.exp(v)),
                        new Optimizer.OptimizationParameter<BoostControlCombined>("kD", Math.log(kD), 2, -15, 10,
                                (c, v) -> c.kD = Math.exp(v)),

                        new Optimizer.OptimizationParameter<BoostControlCombined>("aL", Math.log(alphaLast), 2, -10, 0,
                                (c, v) -> c.alphaLast = Math.exp(v)),
                        new Optimizer.OptimizationParameter<BoostControlCombined>("aF", Math.log(alphaFactor), 2, -5, 5,
                                (c, v) -> c.alphaFactor = Math.exp(v))

                ),
                circuitSuppliers);
    }

    @Override
    public double targetValue(double instant) {
        return targetVoltage.get(instant);
    }

    @Override
    public double actualValue() {
        // return circuit.outputVoltage.get();
        return measuredVoltage;
    }

    @Override
    public String parameterInfo() {
        return String.format("  kP: %.3e   kI: %.3e   kD: %.3e   aL: %.3e   aF: %.3e", kP, kI, kD, alphaLast,
                alphaFactor);
    }

    @Override
    public double simulationDuration() {
        return 800 / controlFrequency;
    }

    @Override
    public double eventTime() {
        return 100 / controlFrequency;
    }

    @Override
    public void initializeSteadyState() {
        var inputVoltage = circuit.source.voltage.get(0);
        var outputVoltage = targetVoltage.get(0);
        var chargeTime = circuit.power.inductance * peakCurrent / inputVoltage;
        var dischargeTime = circuit.power.inductance * peakCurrent / (outputVoltage - inputVoltage);
        var outputPower = outputVoltage * circuit.load.calculateCurrent(outputVoltage, 0);
        var boundaryInputPower = inputVoltage * peakCurrent / 2; // conductor constantly charges and discharges,
                                                                 // constantly using power from the input

        if (outputPower > boundaryInputPower) {
            throw new RuntimeException("CCM Mode not yet implemented");
        } else {
            // (chargeTime+dischargeTime)*boundaryInputPower=outputPower*period
            var period = (chargeTime + dischargeTime) * boundaryInputPower / outputPower;
            var frequency = 1 / period;
            if (frequency < minimumSwitchingFrequency) {
                // cycle skipping
                mode = Mode.CYCLE_SKIPPING;
                pwmChannel.disable = true;
            } else {
                // COT
                mode = Mode.COT;
                integral = frequency;
            }
        }

        fillAdcChannel(0, targetVoltage.get(0));
        fillAdcChannel(1, inputVoltage);
    }

    public double setPoint() {
        return frequency;
    }
}
