package com.github.ruediste.digitalSmpsSim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ruediste.digitalSmpsSim.boost.BoostCircuit;
import com.github.ruediste.digitalSmpsSim.boost.BoostControlPID;
import com.github.ruediste.digitalSmpsSim.quantity.Unit;
import com.github.ruediste.digitalSmpsSim.simulation.Plot;
import com.github.ruediste.digitalSmpsSim.simulation.Simulator;

public class Simulations {

    Logger log = LoggerFactory.getLogger(Simulations.class);

    List<Plot> plots = new ArrayList<>();

    private enum Event {
        INPUT_DROP,
        LOAD_DROP,
        SETPOINT_CHANGE,
    }

    private enum Variant {
        MANUAL,
        OPTIMIZE_INDIVIDUAL,
        OPTIMIZE_ALL,
    }

    public void run() {
        var sim = new Simulator();

        var circuitSet = createCircuits();
        switch (Variant.MANUAL) {
            case MANUAL: {
                var first = true;
                for (var circuitSupplier : circuitSet.circuits) {
                    try {
                        var circuit = circuitSupplier.get();
                        if (first) {
                            log.info(circuit.control.parameterInfo());
                            first = false;
                        }
                        sim.simulate(circuit, circuit.control.simulationDuration(), circuit.plots);
                    } catch (Exception e) {
                        log.error("Error in simulation", e);
                    }
                }
            }
                break;
            case OPTIMIZE_INDIVIDUAL:
                for (var circuitSupplier : circuitSet.circuits) {
                    try {
                        var circuit = circuitSupplier.get();
                        var optimization = circuit.control.optimize(List.of(circuitSupplier));
                        optimization.accept(circuit);
                        sim.simulate(circuit, circuit.control.simulationDuration(), circuit.plots);
                    } catch (Exception e) {
                        log.error("Error in simulation", e);
                    }
                }
                break;
            case OPTIMIZE_ALL: {
                var optimization = circuitSet.circuits.get(0).get().control.optimize(circuitSet.circuits);
                boolean first = true;
                for (var circuitSupplier : circuitSet.circuits) {
                    var circuit = circuitSupplier.get();
                    optimization.accept(circuit);
                    if (first) {
                        first = false;
                        log.info(circuit.control.parameterInfo());
                    }
                    sim.simulate(circuit, circuit.control.simulationDuration(), circuit.plots);
                }
            }
                break;
            default:
                break;

        }
        this.plots = circuitSet.plots;
    }

    public static class CircuitSet {
        List<Plot> plots = new ArrayList<>();
        List<Supplier<BoostCircuit>> circuits = new ArrayList<>();

        public Plot plot(String title) {
            var plot = new Plot();
            plot.title = title;
            this.plots.add(plot);
            return plot;
        }
    }

    public CircuitSet createCircuits() {
        CircuitSet result = new CircuitSet();
        // var design = new BoostDesign();
        // design.outputRipple = Voltage.of(0.1);

        double vIn = 5;
        List<Double> vOuts = List.of(12.);
        List<Double> iOuts = List.of(0.1);
        // List<Double> vOuts = List.of(7., 10., 20., 30.);
        // List<Double> iOuts = List.of(0.01, 0.1, 1., 2.);

        for (var event : Event.values()) {
            for (var vOut : vOuts)
                for (var iOut : iOuts) {
                    // if (iOut.value() >= 1.)
                    // continue;
                    // if (event != Event.INPUT_DROP || vOut.value() != 10 || iOut.value() != 1)
                    // continue;
                    if (event != Event.LOAD_DROP)
                        continue;
                    result.circuits.add(() -> {
                        // var circuit = design.circuit();
                        var circuit = new BoostCircuit();
                        var control = new BoostControlPID(circuit);
                        circuit.control = control;

                        circuit.source.voltage.set(0, vIn);
                        circuit.load.resistance.set(0, vOut / iOut);
                        circuit.outputVoltage.set(vOut);
                        control.targetVoltage.set(0, vOut);

                        control.initializeSteadyState();

                        double eventTime = circuit.control.eventTime();

                        var plot = result.plot(vOut + " - " + iOut)
                                .add("Vout", Unit.Volt, circuit.outputVoltage)
                                .add("IL", Unit.Ampere, circuit.inductorCurrent)
                        // .add("d", Unit.Number, circuit.duty)
                        // .add("sw", Unit.Number, () -> (Double) (circuit.switchOn.get() ? 1. : 0.))
                        // .add("di", circuit.control.di)
                        // .add("Error", circuit.control.errorOut)
                        // .add("Cost", Unit.Number, () -> circuit.costCalculator.currentCost)
                        ;
                        switch (event) {
                            case INPUT_DROP:
                                circuit.source.voltage.set(eventTime, vIn - 1);
                                break;
                            case LOAD_DROP:
                                circuit.load.resistance.set(eventTime,
                                        circuit.load.resistance.get(0) * 1.5);
                                break;
                            case SETPOINT_CHANGE:
                                control.targetVoltage.set(eventTime, vOut * 0.8);
                                break;
                            default:
                                break;

                        }
                        circuit.plots.add(plot);
                        return circuit;
                    });

                }
        }

        return result;
    }

}
