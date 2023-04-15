package com.github.ruediste.digitalSmpsSim.optimization;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
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

import com.github.ruediste.digitalSmpsSim.boost.ControlBase;
import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;
import com.github.ruediste.digitalSmpsSim.simulation.Simulator;

public class Optimizer {

    public static class OptimizationParameter<TControl> {
        public String name;
        public double initialGuess;
        public double sigma;
        public double lowerBound;
        public double upperBound;
        public BiConsumer<TControl, Double> apply;

        public OptimizationParameter(String name, double initialGuess, double sigma, double lowerBound,
                double upperBound, BiConsumer<TControl, Double> apply) {
            this.name = name;
            this.initialGuess = initialGuess;
            this.sigma = sigma;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.apply = apply;
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void apply(PowerCircuitBase circuit, List<OptimizationParameter> parameters, double[] point) {
        for (int i = 0; i < point.length; i++) {
            parameters.get(i).apply.accept(circuit.control, point[i]);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <TCircuit extends PowerCircuitBase, TControl extends ControlBase<?>> Consumer<TCircuit> optimize(
            List<OptimizationParameter<TControl>> parameters,
            List<Supplier<TCircuit>> circuitSuppliers) {
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
                        apply(circuit, (List) parameters, point);
                        sim.simulate(circuit, circuit.control.simulationDuration());
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
                    parameters.stream().mapToDouble(x -> x.initialGuess).toArray()),
                    new MaxIter(1000), new MaxEval(1000),
                    new CMAESOptimizer.PopulationSize(15),
                    new CMAESOptimizer.Sigma(parameters.stream().mapToDouble(x -> x.sigma).toArray()),
                    new SimpleBounds(parameters.stream().mapToDouble(x -> x.lowerBound).toArray(),
                            parameters.stream().mapToDouble(x -> x.upperBound).toArray()));
            return circuit -> apply(circuit, (List) parameters, result.getPoint());
        } finally {
            pool.shutdownNow();
        }
    }
}
