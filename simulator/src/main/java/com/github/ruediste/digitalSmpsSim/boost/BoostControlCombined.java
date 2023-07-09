package com.github.ruediste.digitalSmpsSim.boost;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.ruediste.digitalSmpsSim.optimization.Optimizer;
import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;
import com.github.ruediste.digitalSmpsSim.shared.PwmValuesCalculator;
import com.github.ruediste.digitalSmpsSim.simulation.ExponentialMovingStatistic;
import com.github.ruediste.digitalSmpsSim.simulation.StepChangingValue;

public class BoostControlCombined extends ControlBase<BoostCircuit> {

    public BoostControlCombined(BoostCircuit circuit) {
        super(circuit);
    }

    public StepChangingValue<Double> targetVoltage = new StepChangingValue<>();

    public double kP = 7.5e-05;
    public double kI = 7.8e-05;
    public double kD = 1.4e-05;

    public double alphaLast = 1;
    public double alphaFactor = 1;
    // public double alphaLast = 0.01;
    // public double alphaFactor = 10;

    public double cycleSkippingFrequencyFactor = 3;
    public double controlFrequency = 10e3;
    public double peakCurrent = 60e-3;
    public double idleFraction = 0.1;
    public double startupVoltageFactor = 1.1;

    public double integral;
    public double frequency;
    public double outputCurrentOrig;

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

    private double calculateCotLimitTime() {
        return 1 / (controlFrequency / 2);
    }

    private double calculateCurrent(double fallTime, double period) {
        return peakCurrent * fallTime / (2 * period);
    }

    public Mode mode;
    double lastError;
    double pwmEnabledTime;
    double underFrequencyCycles;
    public int error;
    public double diff;
    public double errorS;

    public double minTime;

    public ExponentialMovingStatistic vOutAdcStats = new ExponentialMovingStatistic(5, 1 / controlFrequency);

    private void control(double instant) {

        int vOutAdc = (int) readAdcChannel(0, 1, x -> random.nextGaussian(voltageToAdc(x), 4.48));
        vOutAdcStats.add(vOutAdc);
        double vOut = adcToVoltage(vOutAdc);
        int vInAdc = (int) readAdcChannel(1, 1, x -> random.nextGaussian(voltageToAdc(x), 4.48));
        double vIn = adcToVoltage(vInAdc);
        double cotLimitTime = calculateCotLimitTime();

        // v=L*di/dt; t=L*iPeak/vIn
        double onTime = circuit.power.inductance * peakCurrent / vIn;
        double fallTime = circuit.power.inductance * peakCurrent / (Math.max(vOut, vIn * startupVoltageFactor) - vIn);
        minTime = (onTime + fallTime) / (1 - idleFraction);
        double iOutMax = calculateCurrent(fallTime, minTime);

        double iCotLimit = calculateCurrent(fallTime, cotLimitTime);
        int targetVoltageAdc = voltageToAdc(targetVoltage.get(instant));

        error = (targetVoltageAdc - vOutAdc);

        if (cotLimitTime < minTime) {
            mode = Mode.CYCLE_SKIPPING;
            pwmEnabledTime = 0;
        }

        switch (mode) {
            case COT: {
                // limit integral
                integral = Math.max(Math.min(integral, iOutMax), iCotLimit);

                errorS = alphaLast * alphaFactor * error + (1 - alphaLast * alphaFactor) * errorS;
                integral += kI * errorS;
                diff = errorS - lastError;

                double outputCurrent = errorS * kP + integral + diff * kD;
                outputCurrentOrig = outputCurrent;

                // limit output current
                if (outputCurrent > iOutMax) {
                    // we are in over current mode
                    outputCurrent = iOutMax;
                }
                if (outputCurrent < iCotLimit) {
                    // we are below the switching current
                    underFrequencyCycles++;
                    outputCurrent = iCotLimit;
                    pwmChannel.disable = true;
                } else {
                    underFrequencyCycles = 0;
                    pwmChannel.disable = false;
                }

                double cycleTime = peakCurrent * fallTime / (2 * outputCurrent);
                frequency = 1 / cycleTime;
                if (underFrequencyCycles > 5) {
                    mode = Mode.CYCLE_SKIPPING;
                    pwmChannel.disable = false;
                    pwmEnabledTime = 0;
                } else {
                    var calc = new PwmValuesCalculator();
                    var values = calc.calculate(frequency, onTime / cycleTime);
                    pwmTimer.prescale = values.prescale;
                    pwmTimer.reload = values.reload;
                    pwmChannel.compare = values.compare;
                }

                lastError = alphaLast * error + (1 - alphaLast) * lastError;
            }
                break;
            case CYCLE_SKIPPING: {
                double period = Math.max(minTime, cotLimitTime / 2);
                frequency = 1 / period;
                var calc = new PwmValuesCalculator();

                var values = calc.calculate(frequency, onTime / period);
                pwmTimer.prescale = values.prescale;
                pwmTimer.reload = values.reload;
                pwmChannel.compare = values.compare;

                if (vOutAdc > targetVoltageAdc) {
                    pwmChannel.disable = true;
                    pwmEnabledTime = 0;
                } else {
                    pwmChannel.disable = false;
                    pwmEnabledTime += (1 / controlFrequency) / period;
                    if (pwmEnabledTime > 6 && error * error > 2 * vOutAdcStats.variance) {
                        mode = Mode.COT;
                        integral = calculateCurrent(fallTime, period);
                        errorS = error;
                        lastError = error;
                        underFrequencyCycles = 0;
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
                                (c, v) -> c.kD = Math.exp(v))

                // new Optimizer.OptimizationParameter<BoostControlCombined>("aL",
                // Math.log(alphaLast), 2, -10, 0,
                // (c, v) -> c.alphaLast = Math.exp(v)),
                // new Optimizer.OptimizationParameter<BoostControlCombined>("aF",
                // Math.log(alphaFactor), 2, 0, 5,
                // (c, v) -> c.alphaFactor = Math.exp(v))

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
        return 400 / controlFrequency;
    }

    @Override
    public double eventTime() {
        return 100 / controlFrequency;
    }

    @Override
    public void initializeSteadyState() {
        var inputVoltage = circuit.inputVoltage.get();
        var outputVoltage = Math.max(circuit.outputVoltage.get(), inputVoltage * startupVoltageFactor);
        var chargeTime = circuit.power.inductance * peakCurrent / inputVoltage;
        var dischargeTime = circuit.power.inductance * peakCurrent / (outputVoltage - inputVoltage);
        var outputCurrent = circuit.load.calculateCurrent(outputVoltage, 0);
        // var outputPower = outputVoltage * outputCurrent;
        var minCycleTime = (chargeTime + dischargeTime) / (1 - idleFraction);
        var maxOutputCurrent = peakCurrent * dischargeTime / (2 * minCycleTime);
        vOutAdcStats.average = voltageToAdc(outputVoltage);

        // limit the output power
        outputCurrent = Math.min(outputCurrent, maxOutputCurrent);

        {
            var period = peakCurrent * dischargeTime / (2 * outputCurrent);
            if (period > calculateCotLimitTime()) {
                // cycle skipping
                mode = Mode.CYCLE_SKIPPING;
                pwmChannel.disable = true;
            } else {
                // COT
                mode = Mode.COT;
                integral = outputCurrent;
            }
        }

        fillAdcChannel(0, targetVoltage.get(0));
        fillAdcChannel(1, inputVoltage);
    }

    public double setPoint() {
        return frequency;
    }
}
