package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.quantity.Current;
import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Unitless;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.Circuit;
import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;
import com.github.ruediste.digitalSmpsSim.simulation.ElementInput;
import com.github.ruediste.digitalSmpsSim.simulation.ElementOutput;

public class CostCalculator extends CircuitElement {
    public ElementInput<Voltage> outputVoltage = new ElementInput<>(this) {
    };
    public ElementInput<Current> inductorCurrent = new ElementInput<>(this) {
    };
    public ElementOutput<Unitless> currentCost = new ElementOutput<>(this) {
    };

    public CostCalculator(Circuit circuit) {
        super(circuit);
    }

    public Voltage targetVoltage;
    public Duration evaluationPeriod;

    public Instant firstEvaluation = Instant.of(0);
    public Instant nextEvaluation;

    public double totalCost;
    double kError = 1;
    double kDiff = 0;
    double kCurrent = 0;

    double alpha = 0.1;

    double avgOutputVoltage;
    double avgCurrent;

    @Override
    public void initialize() {
        nextEvaluation = firstEvaluation;
        avgOutputVoltage = targetVoltage.value();
        avgCurrent = 0;
        currentCost.set(Unitless.of(0));
    }

    @Override
    public void run(Instant stepStart, Instant stepEnd, Duration stepDuration) {
        if (stepStart.compareTo(nextEvaluation) >= 0) {
            var value = outputVoltage.get().value();
            var current = inductorCurrent.get().value();
            var cost = 0.;

            // error squared
            cost += kError * Math.pow(value - targetVoltage.value(), 2);

            // differentiation
            cost += kDiff * Math.pow(value - avgOutputVoltage, 2);

            // difference from avg current
            if (avgCurrent > 1e-3) {
                var currentRatio = current / avgCurrent;
                if (currentRatio < 1)
                    currentRatio = 1 - currentRatio;
                cost += kCurrent * (currentRatio - 1);
            }

            if (stepStart.value() < 10e-3) {
                cost = 0;
            }

            totalCost += cost;
            currentCost.set(Unitless.of(cost));

            nextEvaluation = nextEvaluation.add(evaluationPeriod);
            avgOutputVoltage = value * alpha + (1 - alpha) * avgOutputVoltage;
            avgCurrent = current * alpha + (1 - alpha) * avgCurrent;
        }
    }
}
