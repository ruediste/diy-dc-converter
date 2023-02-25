package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.quantity.Current;
import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Fraction;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Unitless;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.Circuit;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.github.ruediste.digitalSmpsSim.simulation.ElementInput;
import com.github.ruediste.digitalSmpsSim.simulation.ElementOutput;
import com.github.ruediste.digitalSmpsSim.simulation.StepChangingValue;

public class CostCalculator extends CircuitElement {
    public ElementInput<Voltage> outputVoltage = new ElementInput<>(this) {
    };
    public ElementInput<Current> inductorCurrent = new ElementInput<>(this) {
    };
    public ElementInput<Fraction> duty = new ElementInput<>(this) {
    };
    public ElementOutput<Unitless> currentCost = new ElementOutput<>(this) {
    };

    public CostCalculator(Circuit circuit) {
        super(circuit);
    }

    public StepChangingValue<Voltage> targetVoltage;

    public Duration evaluationPeriod;

    public Instant firstEvaluation = Instant.of(0);
    public Instant nextEvaluation;

    public double totalCost;
    double kError = 1;
    double kDiff = 1;
    double kDiffDuty = 1000;
    double kCurrent = 0;

    double alpha = 0.1;

    double avgOutputVoltage;
    double avgCurrent;
    double avgDuty;

    @Override
    public void initialize() {
        nextEvaluation = firstEvaluation;
        avgOutputVoltage = targetVoltage.get(Instant.of(0)).value();
        avgCurrent = 0;
        currentCost.set(Unitless.of(0));
        avgDuty = 0;
    }

    @Override
    public void run(Instant stepStart, Instant stepEnd, Duration stepDuration) {
        if (stepStart.compareTo(nextEvaluation) >= 0) {
            var value = outputVoltage.get().value();
            var current = inductorCurrent.get().value();

            var cost = 0.;

            // error squared
            {
                var tmp = value / targetVoltage.get(stepStart).value();
                if (tmp < 1) {
                    tmp = Math.max(0.1, 1 / tmp);
                }
                cost += kError * (Math.pow(tmp, 2)
                        - 1);
            }

            // differentiation
            cost += kDiff * Math.pow(value - avgOutputVoltage, 2);

            // difference from avg current
            if (avgCurrent > 1e-3) {
                var currentRatio = current / avgCurrent;
                if (currentRatio < 1)
                    currentRatio = 1 - currentRatio;
                cost += kCurrent * (currentRatio - 1);
            }

            // duty diff
            cost += kDiffDuty * Math.pow(duty.get().value() - avgDuty, 2);

            if (stepStart.value() < 1e-3) {
                cost = 0;
            }

            totalCost += cost;
            currentCost.set(Unitless.of(cost));

            nextEvaluation = nextEvaluation.add(evaluationPeriod);
            avgOutputVoltage = value * alpha + (1 - alpha) * avgOutputVoltage;
            avgCurrent = current * alpha + (1 - alpha) * avgCurrent;
            avgDuty = duty.get().value() * alpha + (1 - alpha) * avgDuty;
        }
    }
}
