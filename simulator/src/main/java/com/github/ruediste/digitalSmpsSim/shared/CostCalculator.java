package com.github.ruediste.digitalSmpsSim.shared;

import com.github.ruediste.digitalSmpsSim.simulation.CircuitElement;

public class CostCalculator extends CircuitElement {
    private PowerCircuitBase circuit;

    public CostCalculator(PowerCircuitBase circuit) {
        super(circuit);
        this.circuit = circuit;
    }

    public double totalCost;
    public double currentCost;

    double kError = 1;
    double kErrorSettle = 1e1;
    double kDiff = 0;
    double kDiffSettle = 0;
    double kDiffSetpoint = 0;
    double kDiffSetpointSettle = 0;
    double kMaxError2 = 0;

    double alpha = 1e3;
    public double avgOutputVoltage;
    double lastSetpoint;
    double maxError2 = 0;
    double settleStart;

    @Override
    public void postInitialize() {
        avgOutputVoltage = circuit.control.targetValue(0);
        lastSetpoint = circuit.control.setPoint();
        settleStart = circuit.control.simulationDuration() / 2;
    }

    @Override
    public void run(double stepStart, double stepEnd, double stepDuration) {
        double settle = stepStart > settleStart ? 1 : 0;

        var value = circuit.control.actualValue();

        var cost = 0.;

        // error squared
        double error2 = Math.pow(value - circuit.control.targetValue(stepEnd), 2);
        cost += (kError + settle * kErrorSettle) * error2;
        maxError2 = Math.max(maxError2, error2);

        // differentiation
        cost += (kDiff + settle * kDiffSettle) * Math.pow(value - avgOutputVoltage, 2);

        // setpoint diff
        cost += (kDiffSetpoint + settle * kDiffSetpointSettle)
                * Math.pow(circuit.control.setPoint() - lastSetpoint, 2);

        totalCost += cost * stepDuration;
        currentCost = cost;

        avgOutputVoltage = value * alpha * stepDuration + (1 - alpha * stepDuration) * avgOutputVoltage;
        lastSetpoint = circuit.control.setPoint();
    }

    @Override
    public void finish() {
        totalCost += kMaxError2 * maxError2;
    }
}
