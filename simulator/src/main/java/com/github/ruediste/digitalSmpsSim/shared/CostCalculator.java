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
    double kDiffSetpoint = 2e-5;
    double kMaxError2 = 0;

    double alpha = 0.1;

    double avgOutputVoltage;
    double lastSetpoint;
    double maxError2 = 0;

    @Override
    public void postInitialize() {
        avgOutputVoltage = circuit.control.targetValue(0);
        lastSetpoint = circuit.control.setPoint();
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

            // setpoint diff
            cost += kDiffSetpoint * Math.pow(circuit.control.setPoint() - lastSetpoint, 2);

            totalCost += cost * stepDuration;
            currentCost = cost;

            avgOutputVoltage = value * alpha + (1 - alpha) * avgOutputVoltage;
            lastSetpoint = circuit.control.setPoint();

            nextEvaluation += evaluationPeriod;
        }
    }

    @Override
    public void finish() {
        totalCost += kMaxError2 * maxError2;
    }
}
