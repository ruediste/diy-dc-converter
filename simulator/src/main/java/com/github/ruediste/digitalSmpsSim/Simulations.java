package com.github.ruediste.digitalSmpsSim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ruediste.digitalSmpsSim.boost.BoostCircuit;
import com.github.ruediste.digitalSmpsSim.boost.BoostControlCot;
import com.github.ruediste.digitalSmpsSim.boost.BoostControlCot.Mode;
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
        LOAD_CHANGE
    }

    public record CircuitParameterValue(
            CircuitParameterAxis axis,
            Object value,
            String label) {
    };

    private enum Event {
        NONE,
        INPUT_DROP,
        LOAD_CHANGE,
        SETPOINT_CHANGE,
        POWER_ON,
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
                        var parameterSetter = circuit.control.optimize(List.of(circuitSupplier));
                        parameterSetter.accept(circuit);
                        sim.simulate(circuit, circuit.control.simulationDuration(), circuit.plots);
                        this.circuits.add(circuit);
                    } catch (Exception e) {
                        log.error("Error in simulation", e);
                    }
                }
                break;
            case OPTIMIZE_ALL: {
                var parameterSetter = circuitSuppliers.get(0).get().control.optimize(circuitSuppliers);
                boolean first = true;
                for (var circuitSupplier : circuitSuppliers) {
                    var circuit = circuitSupplier.get();
                    parameterSetter.accept(circuit);
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
        List<Double> vOuts = List.of(12., 20.);
        List<Double> iOuts = List.of(0.010, 0.001);
        List<Double> loadChanges = List.of(2., 1.2, 0.8, 0.5, 0.2, 0.1);
        // List<Double> vOuts = List.of(7., 10., 20., 30.);
        // List<Double> iOuts = List.of(0.01, 0.1, 1., 2.);

        for (var event : Event.values()) {
            for (var vOut : vOuts)
                for (var loadChange : event == Event.LOAD_CHANGE ? loadChanges : List.of(1.))
                    for (var iOut : iOuts) {
                        if (event != Event.NONE
                                && event != Event.LOAD_CHANGE
                        // && event != Event.POWER_ON
                        )
                            continue;
                        result.add(() -> {
                            // var circuit = design.circuit();
                            var circuit = new BoostCircuit();
                            var control = new BoostControlCot(circuit);
                            circuit.control = control;

                            circuit.addParameterValue(CircuitParameterAxis.EVENT, event, event.toString());
                            circuit.addParameterValue(CircuitParameterAxis.V_OUT, vOut, SiPrefix.format(vOut, "V"));
                            circuit.addParameterValue(CircuitParameterAxis.I_OUT, iOut, SiPrefix.format(iOut, "A"));
                            if (event == Event.LOAD_CHANGE) {
                                circuit.addParameterValue(CircuitParameterAxis.LOAD_CHANGE, loadChange,
                                        "" + loadChange);

                            }

                            circuit.source.voltage.set(0, vIn);
                            circuit.load.resistance.set(0, vOut / iOut);
                            circuit.outputVoltage.initialize(vOut);
                            circuit.inputVoltage.initialize(vIn);
                            control.targetVoltage.set(0, vOut);

                            control.initializeSteadyState();

                            double eventTime = circuit.control.eventTime();

                            new Plot(circuit, vOut + " - " + iOut)
                                    .add("Vout", Unit.Volt, circuit.outputVoltage)
                                    // .add("IL", Unit.Ampere, circuit.inductorCurrent)
                                    .add("Error", Unit.Number, () -> (double) control.error)// .combinedAxis()
                                    // .add("ErrorS", Unit.Number, () -> (double) control.errorS).combinedAxis()
                                    // .add("Int", Unit.Ampere, () -> (double) control.integral)
                                    // .add("Io", Unit.Ampere, () -> (double) control.outputCurrentOrig)

                                    .add("Mode", Unit.Number, () -> control.mode == Mode.CYCLE_SKIPPING ? 0. : 1.)

                                    .add("MaxFreq", Unit.Hertz, () -> (double) 1 / control.minTime).combinedAxis()
                                    .add("Frequency", Unit.Hertz, () -> (double) control.frequency).combinedAxis()
                            // .add("Duty", Unit.Number, circuit.duty)
                            // .add("sw", Unit.Number, () -> (Double) (circuit.switchOn.get() ? 1. : 0.))
                            // .add("PWM Enabled", Unit.Number,
                            // () -> (Double) (control.pwmChannel.disable ? 0. : 1.))
                            // .add("int", Unit.Number, () -> (double) control.integral)
                            // .add("Vm", Unit.Number, () -> (double) control.measuredVoltage)
                            // .add("Error", circuit.control.errorOut)
                            // .add("Cost", Unit.Number, () -> circuit.costCalculator.currentCost)
                            // .add("tCost", Unit.Number, () -> circuit.costCalculator.totalCost)
                            // .add("cAvgO", Unit.Volt, () -> circuit.costCalculator.avgOutputVoltage)
                            //

                            ;
                            switch (event) {
                                case INPUT_DROP:
                                    circuit.source.voltage.set(eventTime, vIn - 1);
                                    break;
                                case LOAD_CHANGE:
                                    circuit.load.resistance.set(eventTime,
                                            circuit.load.resistance.get(0) / loadChange);
                                    break;
                                case SETPOINT_CHANGE:
                                    control.targetVoltage.set(eventTime, vOut * 1.2);
                                    break;
                                case POWER_ON:
                                    circuit.outputVoltage.initialize(vIn);
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
