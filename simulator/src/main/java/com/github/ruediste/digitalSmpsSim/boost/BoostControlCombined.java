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

    public double kP = 1e-04;
    public double kI = 2e-06;
    public double kD = 2e-05;

    public double alphaLast = 0.01;
    public double alphaFactor = 10;

    public double cycleSkippingFrequencyFactor = 3;
    public double controlFrequency = 5e3;
    public double peakCurrent = 60e-3;
    public double idleFraction = 0.1;

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

    private double cotLimitTime() {
        return 1 / (controlFrequency / 2);
    }

    public Mode mode;
    double lastError;
    double pwmEnabledCycles;
    double underFrequencyCycles;
    public int error;
    public double diff;
    public double errorS;

    public double minTime;

    private void control(double instant) {

        int vOut = (int) readAdcChannel(0, 1, x -> random.nextGaussian(voltageToAdc(x), 4.48));
        int vIn = (int) readAdcChannel(1, 1, x -> random.nextGaussian(voltageToAdc(x), 4.48));

        // v=L*di/dt; t=L*iPeak/vIn
        double onTime = circuit.power.inductance * peakCurrent / adcToVoltage(vIn);
        double fallTime = circuit.power.inductance * peakCurrent / (adcToVoltage(vOut) - adcToVoltage(vIn));
        minTime = (onTime + fallTime) / (1 - idleFraction);
        double iOutMax = peakCurrent * fallTime / (2 * minTime);

        double iCotLimit = peakCurrent * fallTime / (2 * cotLimitTime());

        error = (voltageToAdc(targetVoltage.get(instant)) - vOut);

        if (cotLimitTime() < minTime) {
            mode = Mode.CYCLE_SKIPPING;
        }

        switch (mode) {
            case COT: {
                errorS = alphaLast * alphaFactor * error + (1 - alphaLast * alphaFactor) * errorS;
                integral += kI * errorS;
                diff = errorS - lastError;

                double outputCurrent = errorS * kP + integral + diff * kD;
                outputCurrentOrig = outputCurrent;

                // limit integral
                integral = Math.max(Math.min(integral, iOutMax), iCotLimit);

                // limit output current
                if (outputCurrent > iOutMax) {
                    // we are in over current mode
                    outputCurrent = iOutMax;
                }
                if (outputCurrent < iCotLimit) {
                    // we are below the switching current
                    underFrequencyCycles++;
                    outputCurrent = iCotLimit;
                } else {
                    underFrequencyCycles = 0;
                }

                double cycleTime = peakCurrent * fallTime / (2 * outputCurrent);
                frequency = 1 / cycleTime;
                if (underFrequencyCycles > 0) {
                    mode = Mode.CYCLE_SKIPPING;
                    pwmChannel.disable = true;
                    pwmEnabledCycles = 0;
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
                double period = cotLimitTime() / 2;
                frequency = 1 / period;
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
                    if (pwmEnabledCycles > 3) {
                        mode = Mode.COT;
                        integral = iCotLimit;
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
                                (c, v) -> c.kD = Math.exp(v)),

                        new Optimizer.OptimizationParameter<BoostControlCombined>("aL", Math.log(alphaLast), 2, -10, 0,
                                (c, v) -> c.alphaLast = Math.exp(v)),
                        new Optimizer.OptimizationParameter<BoostControlCombined>("aF", Math.log(alphaFactor), 2, 0, 5,
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
        var outputCurrent = circuit.load.calculateCurrent(outputVoltage, 0);
        // var outputPower = outputVoltage * outputCurrent;
        var minCycleTime = (chargeTime + dischargeTime) / (1 - idleFraction);
        var maxOutputCurrent = peakCurrent * dischargeTime / (2 * minCycleTime);

        // limit the output power
        outputCurrent = Math.min(outputCurrent, maxOutputCurrent);

        {
            var period = peakCurrent * dischargeTime / (2 * outputCurrent);
            if (period > cotLimitTime()) {
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
