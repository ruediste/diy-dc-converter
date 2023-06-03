package com.github.ruediste.digitalSmpsSim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ruediste.digitalSmpsSim.boost.BoostCircuit;
import com.github.ruediste.digitalSmpsSim.boost.BoostControlCombined;
import com.github.ruediste.digitalSmpsSim.boost.BoostControlCombined.Mode;
import com.github.ruediste.digitalSmpsSim.quantity.SiPrefix;
import com.github.ruediste.digitalSmpsSim.quantity.Unit;
import com.github.ruediste.digitalSmpsSim.shared.PowerCircuitBase;
import com.github.ruediste.digitalSmpsSim.simulation.Plot;
import com.github.ruediste.digitalSmpsSim.simulation.Simulator;

public class Simulations {

    Logger log = LoggerFactory.getLogger(Simulations.class);

    List<PowerCircuitBase> circuits = new ArrayList<>();

    public enum CircuitParameterAxis {
        EVENT,
        V_OUT,
        I_OUT,
        LOAD_DROP
    }

    public record CircuitParameterValue(
            CircuitParameterAxis axis,
            String label) {
    };

    private enum Event {
        NONE,
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

        var circuitSuppliers = createCircuits();
        switch (Variant.OPTIMIZE_ALL) {
            case MANUAL: {
                var first = true;
                for (var circuitSupplier : circuitSuppliers) {
                    try {
                        var circuit = circuitSupplier.get();
                        if (first) {
                            log.info(circuit.control.parameterInfo());
                            first = false;
                        }
                        sim.simulate(circuit, circuit.control.simulationDuration(), circuit.plots);
                        this.circuits.add(circuit);
                    } catch (Exception e) {
                        log.error("Error in simulation", e);
                    }
                }
            }
                break;
            case OPTIMIZE_INDIVIDUAL:
                for (var circuitSupplier : circuitSuppliers) {
                    try {
                        var circuit = circuitSupplier.get();
                        var optimization = circuit.control.optimize(List.of(circuitSupplier));
                        optimization.accept(circuit);
                        sim.simulate(circuit, circuit.control.simulationDuration(), circuit.plots);
                        this.circuits.add(circuit);
                    } catch (Exception e) {
                        log.error("Error in simulation", e);
                    }
                }
                break;
            case OPTIMIZE_ALL: {
                var optimization = circuitSuppliers.get(0).get().control.optimize(circuitSuppliers);
                boolean first = true;
                for (var circuitSupplier : circuitSuppliers) {
                    var circuit = circuitSupplier.get();
                    optimization.accept(circuit);
                    if (first) {
                        first = false;
                        log.info(circuit.control.parameterInfo());
                    }
                    sim.simulate(circuit, circuit.control.simulationDuration(), circuit.plots);
                    this.circuits.add(circuit);
                }
            }
                break;
            default:
                break;

        }
    }

    public List<Supplier<BoostCircuit>> createCircuits() {
        List<Supplier<BoostCircuit>> result = new ArrayList<>();
        // var design = new BoostDesign();
        // design.outputRipple = Voltage.of(0.1);

        double vIn = 5;
        List<Double> vOuts = List.of(12.);
        List<Double> iOuts = List.of(10e-3);
        List<Double> loadDrops = List.of(0.8, 2., 5., 10., 100.);
        // List<Double> vOuts = List.of(7., 10., 20., 30.);
        // List<Double> iOuts = List.of(0.01, 0.1, 1., 2.);

        for (var event : Event.values()) {
            for (var vOut : vOuts)
                for (var loadDrop : event == Event.LOAD_DROP ? loadDrops : List.of(1.))
                    for (var iOut : iOuts) {
                        // if (iOut.value() >= 1.)
                        // continue;
                        // if (event != Event.LOAD_DROP)
                        // continue;
                        if (event != Event.LOAD_DROP && event != Event.NONE)
                            continue;
                        result.add(() -> {
                            // var circuit = design.circuit();
                            var circuit = new BoostCircuit();
                            var control = new BoostControlCombined(circuit);
                            circuit.control = control;

                            circuit.addParameterValue(CircuitParameterAxis.EVENT, event.toString());
                            circuit.addParameterValue(CircuitParameterAxis.V_OUT, SiPrefix.format(vOut, "V"));
                            circuit.addParameterValue(CircuitParameterAxis.I_OUT, SiPrefix.format(iOut, "A"));
                            if (event == Event.LOAD_DROP) {
                                circuit.addParameterValue(CircuitParameterAxis.LOAD_DROP, "" + loadDrop);

                            }

                            circuit.source.voltage.set(0, vIn);
                            circuit.load.resistance.set(0, vOut / iOut);
                            circuit.outputVoltage.initialize(vOut);
                            control.targetVoltage.set(0, vOut);

                            control.initializeSteadyState();

                            double eventTime = circuit.control.eventTime();

                            new Plot(circuit, vOut + " - " + iOut)
                                    .add("Vout", Unit.Volt, circuit.outputVoltage)
                                    // .add("IL", Unit.Ampere, circuit.inductorCurrent)
                                    // .add("Error", Unit.Number, () -> (double) control.error)
                                    .add("Mode", Unit.Number, () -> control.mode == Mode.CYCLE_SKIPPING ? 0. : 1.)
                                    // .add("Diff", Unit.Number, () -> (double) control.diff)
                                    .add("Frequency", Unit.Hertz, () -> (double) control.frequency)
                                    // .add("Duty", Unit.Number, circuit.duty)
                                    // .add("sw", Unit.Number, () -> (Double) (circuit.switchOn.get() ? 1. : 0.))
                                    .add("PWM Enabled", Unit.Number,
                                            () -> (Double) (control.pwmChannel.disable ? 0. : 1.))
                            // .add("int", Unit.Number, () -> (double) control.integral)
                            // .add("Vm", Unit.Number, () -> (double) control.measuredVoltage)
                            // .add("Error", circuit.control.errorOut)
                            // .add("Cost", Unit.Number, () -> circuit.costCalculator.currentCost)
                            // .add("tCost", Unit.Number, () -> circuit.costCalculator.totalCost)
                            //
                            ;
                            switch (event) {
                                case INPUT_DROP:
                                    circuit.source.voltage.set(eventTime, vIn - 1);
                                    break;
                                case LOAD_DROP:
                                    circuit.load.resistance.set(eventTime,
                                            circuit.load.resistance.get(0) * loadDrop);
                                    break;
                                case SETPOINT_CHANGE:
                                    control.targetVoltage.set(eventTime, vOut * 0.8);
                                    break;
                                default:
                                    break;

                            }
                            return circuit;
                        });

                    }
        }

        return result;
    }

}
