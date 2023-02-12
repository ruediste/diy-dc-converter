package com.github.ruediste.digitalSmpsSim.optimization;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;

import com.github.ruediste.digitalSmpsSim.boost.BoostCircuit;
import com.github.ruediste.digitalSmpsSim.simulation.Simulator;

public class Optimizer {
    private void apply(BoostCircuit circuit, double[] point) {
        circuit.control.kP = Math.exp(point[0]);
        circuit.control.kI = Math.exp(point[1]);
        circuit.control.kD = Math.exp(point[2]);
        // circuit.control.lowPass = Math.exp(Math.max(-5, point[2])) + 2;
    }

    public Consumer<BoostCircuit> optimize(BoostCircuit initialCircuit,
            Iterable<Supplier<BoostCircuit>> circuitSuppliers,
            long simulationPeriods) {
        Simulator sim = new Simulator();
        PowellOptimizer opt = new PowellOptimizer(1e-3, 1e-8);
        var result = opt.optimize(GoalType.MINIMIZE, new ObjectiveFunction(new MultivariateFunction() {

            @Override
            public double value(double[] point) {
                var cost = 0.;
                for (var circuitSupplier : circuitSuppliers) {
                    var circuit = circuitSupplier.get();
                    apply(circuit, point);
                    sim.simulate(circuit, circuit.switchingPeriod() * simulationPeriods);
                    cost += circuit.costCalculator.totalCost;
                }
                return cost;
            }
        }), new InitialGuess(
                new double[] {
                        Math.log(initialCircuit.control.kP),
                        Math.log(initialCircuit.control.kI),
                        Math.log(initialCircuit.control.kD),
                // Math.log(initialCircuit.control.lowPass - 2),
                }),
                new MaxIter(1000), new MaxEval(1000));
        return circuit -> apply(circuit, result.getPoint());
    }
}
