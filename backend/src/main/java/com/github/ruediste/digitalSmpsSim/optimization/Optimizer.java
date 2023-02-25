package com.github.ruediste.digitalSmpsSim.optimization;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.JDKRandomGenerator;

import com.github.ruediste.digitalSmpsSim.boost.BoostCircuit;
import com.github.ruediste.digitalSmpsSim.simulation.Simulator;

public class Optimizer {
    private void apply(BoostCircuit circuit, double[] point) {
        circuit.control.kP = Math.exp(point[0]);
        circuit.control.kI = Math.exp(point[1]);
        circuit.control.kD = Math.exp(point[2]);
        // circuit.control.lowPass = Math.exp(Math.max(-5, point[3])) + 2;
    }

    public Consumer<BoostCircuit> optimize(BoostCircuit initialCircuit,
            List<Supplier<BoostCircuit>> circuitSuppliers,
            long simulationPeriods) {
        Simulator sim = new Simulator();
        // PowellOptimizer opt = new PowellOptimizer(1e-3, 1e-8);
        var opt = new CMAESOptimizer(10000, 0, false, 10, 10, new JDKRandomGenerator(0), false,
                new SimplePointChecker<>(1e-2, -1));
        var pool = Executors.newFixedThreadPool(8);
        try {
            var result = opt.optimize(GoalType.MINIMIZE, new ObjectiveFunction(new MultivariateFunction() {

                @Override
                public double value(double[] point) {
                    return circuitSuppliers.stream().map(circuitSupplier -> pool.submit(() -> {
                        var circuit = circuitSupplier.get();
                        apply(circuit, point);
                        sim.simulate(circuit, circuit.switchingPeriod() * simulationPeriods);
                        return circuit.costCalculator.totalCost;
                    })).toList().stream().collect(Collectors.summingDouble(x -> {
                        try {
                            return x.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }));
                }
            }), new InitialGuess(
                    new double[] {
                            Math.log(initialCircuit.control.kP),
                            Math.log(initialCircuit.control.kI),
                            Math.log(initialCircuit.control.kD),
                    // Math.log(initialCircuit.control.lowPass - 2),
                    }),
                    new MaxIter(1000), new MaxEval(1000),
                    new CMAESOptimizer.PopulationSize(15),
                    new CMAESOptimizer.Sigma(new double[] { 5, 5, 5 }),
                    new SimpleBounds(new double[] { -10, -10, -10 }, new double[] { 10, 10, 10 }));
            return circuit -> apply(circuit, result.getPoint());
        } finally {
            pool.shutdownNow();
        }
    }
}
