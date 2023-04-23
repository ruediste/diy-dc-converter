package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;

public class CostCalculator extends CircuitElement {
    private PowerCircuitBase circuit;

    public CostCalculator(PowerCircuitBase circuit) {
        super(circuit);
        this.circuit = circuit;
    }

    public double evaluationPeriod = 1 / 10e3;;

    public double nextEvaluation = 0;

    public double totalCost;
    public double currentCost;

    double kError = 1;
    double kDiff = 0;
    double kDiffDuty = 1;
    double kMaxError2 = 0;

    double alpha = 0.1;

    double avgOutputVoltage;
    double lastDuty;
    double maxError2 = 0;

    @Override
    public void postInitialize() {
        avgOutputVoltage = circuit.control.targetValue(0);
        lastDuty = circuit.duty.get();
    }

    @Override
    public void run(double stepStart, double stepEnd, double stepDuration) {
        while (stepStart > nextEvaluation) {

            var value = circuit.control.actualValue();

            var cost = 0.;

            // error squared
            double error2 = Math.pow(value - circuit.control.targetValue(stepEnd), 2);
            cost += kError * error2;
            maxError2 = Math.max(maxError2, error2);

            // differentiation
            cost += kDiff * Math.pow(value - avgOutputVoltage, 2);

            // duty diff
            cost += kDiffDuty * Math.pow(circuit.duty.get() - lastDuty, 2);

            totalCost += cost * stepDuration;
            currentCost = cost;

            avgOutputVoltage = value * alpha + (1 - alpha) * avgOutputVoltage;
            lastDuty = circuit.duty.get();

            nextEvaluation += evaluationPeriod;
        }
    }

    @Override
    public void finish() {
        totalCost += kMaxError2 * maxError2;
    }
}
