package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;

public class CostCalculator extends CircuitElement {
    private PowerCircuitBase circuit;

    public CostCalculator(PowerCircuitBase circuit) {
        super(circuit);
        this.circuit = circuit;
    }

    public double evaluationPeriod;

    public double firstEvaluation = 0;
    public double nextEvaluation;

    public double totalCost;
    public double currentCost;

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
    }

    @Override
    public void run(double stepStart, double stepEnd, double stepDuration) {
        if (stepStart >= nextEvaluation) {
            var value = circuit.control.actualValue();
            var current = circuit.inductorCurrent.get();

            var cost = 0.;

            // error squared
            {
                var tmp = value / circuit.control.targetValue(stepStart);
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
            cost += kDiffDuty * Math.pow(circuit.control.duty - avgDuty, 2);

            if (stepStart < 1e-3) {
                cost = 0;
            }

            totalCost += cost;
            currentCost = cost;

            nextEvaluation += evaluationPeriod;
            avgOutputVoltage = value * alpha + (1 - alpha) * avgOutputVoltage;
            avgCurrent = current * alpha + (1 - alpha) * avgCurrent;
            avgDuty = circuit.duty.get() * alpha + (1 - alpha) * avgDuty;
        }
    }
}
