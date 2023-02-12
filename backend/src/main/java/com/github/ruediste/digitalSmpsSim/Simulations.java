package com.github.ruediste.digitalSmpsSim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.ruediste.digitalSmpsSim.PlotRest.PlotGroup;
import com.github.ruediste.digitalSmpsSim.boost.BoostCircuit;
import com.github.ruediste.digitalSmpsSim.boost.BoostDesign;
import com.github.ruediste.digitalSmpsSim.optimization.Optimizer;
import com.github.ruediste.digitalSmpsSim.quantity.Current;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.Plot;
import com.github.ruediste.digitalSmpsSim.simulation.Simulator;

import jakarta.annotation.PostConstruct;

@Component
public class Simulations {

    Logger log = LoggerFactory.getLogger(Simulations.class);

    List<PlotGroup> plotGroups = new ArrayList<>();

    private enum Event {
        STEADY,
        INPUT_DROP
    }

    private boolean getTrue() {
        return true;
    }

    private enum Variant {
        MANUAL,
        OPTIMIZE_INDIVIDUAL,
        OPTIMIZE_ALL
    }

    @PostConstruct
    public void run() {
        var sim = new Simulator();
        var optimizer = new Optimizer();

        var circuitSet = createCircuits();
        var variant = Variant.MANUAL;
        switch (variant) {
            case MANUAL: {
                var first = true;
                for (var circuitSupplier : circuitSet.circuits) {
                    try {
                        var circuit = circuitSupplier.get();
                        var simulationPeriods = 4000;
                        if (first) {
                            log.info("kP: {} kI: {} kD: {}", circuit.control.kP, circuit.control.kI,
                                    circuit.control.kD);
                            first = false;
                        }
                        sim.simulate(circuit, circuit.switchingPeriod() * simulationPeriods, circuit.plots);
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
                        var simulationPeriods = 2000;
                        var optimization = optimizer.optimize(circuit, List.of(circuitSupplier), simulationPeriods);
                        optimization.accept(circuit);
                        log.info("kP: {} kI: {} kD: {}", circuit.control.kP, circuit.control.kI, circuit.control.kD);
                        sim.simulate(circuit, circuit.switchingPeriod() * simulationPeriods, circuit.plots);
                    } catch (Exception e) {
                        log.error("Error in simulation", e);
                    }
                }
                break;
            case OPTIMIZE_ALL: {
                var simulationPeriods = 2000;
                var optimization = optimizer.optimize(circuitSet.circuits.get(0).get(), circuitSet.circuits,
                        simulationPeriods);
                boolean first = true;
                for (var circuitSupplier : circuitSet.circuits) {
                    var circuit = circuitSupplier.get();
                    optimization.accept(circuit);
                    if (first) {
                        first = false;
                        log.info("kP: {} kI: {} kD: {}", circuit.control.kP, circuit.control.kI, circuit.control.kD);
                    }
                    sim.simulate(circuit, circuit.switchingPeriod() * simulationPeriods, circuit.plots);
                }
            }
                break;
            default:
                break;

        }
        this.plotGroups = circuitSet.plotGroups;
    }

    public static class CircuitSet {
        List<PlotGroup> plotGroups = new ArrayList<>();
        List<Supplier<BoostCircuit>> circuits = new ArrayList<>();

        public PlotGroup plotGroup(String label) {
            var group = new PlotGroup(label);
            this.plotGroups.add(group);
            return group;
        }

        public Plot plot(PlotGroup group, String title) {
            var plot = new Plot(group);
            plot.title = title;
            return plot;
        }
    }

    public CircuitSet createCircuits() {
        CircuitSet result = new CircuitSet();
        var design = new BoostDesign();
        design.outputRipple = Voltage.of(0.1);

        List<Voltage> vOuts = List.of(6., 10., 20., 40.).stream().map(v -> Voltage.of(v)).toList();
        List<Current> iOuts = List.of(0.01, 0.1, 1., 2.).stream().map(v -> Current.of(v)).toList();

        for (var event : Event.values()) {
            var plotGroup = result.plotGroup(event.toString());
            for (var vOut : vOuts)
                for (var iOut : iOuts) {
                    if (event == Event.STEADY)
                        continue;
                    // if (event != Event.INPUT_DROP || vOut.value() != 10 || iOut.value() != 1)
                    // continue;
                    result.circuits.add(() -> {
                        var circuit = design.circuit();
                        circuit.load.resistance = vOut.divide(iOut);
                        circuit.power.vCap = vOut;
                        circuit.control.targetVoltage = vOut;
                        circuit.costCalculator.targetVoltage = vOut;

                        var iLAvg = Current.of(iOut.value() * vOut.value() / design.inputVoltage.value());
                        // Vo= Vin/(1-D); 1-D=Vin/Vo; D-1=-Vin/Vo; D=1-Vin/Vo;
                        var ccmDuty = 1 - design.inputVoltage.value() / vOut.value();
                        var iLRipple = design.inputVoltage.value() * ccmDuty * design.switchingPeriod().value()
                                / circuit.power.inductance;

                        if (iLAvg.value() > iLRipple / 2) {
                            // CCM Mode
                            circuit.power.iL = iLAvg.minus(iLRipple / 2);
                            circuit.control.duty = ccmDuty;
                        } else {
                            // DCM Mode
                            circuit.power.iL = Current.of(0);

                            // time required to get to twice the average input current
                            var tOn = circuit.power.inductance * iLAvg.value() * 2 / design.inputVoltage.value();

                            circuit.control.duty = tOn / design.switchingPeriod().value();
                        }

                        var plot = result.plot(plotGroup, vOut + " - " + iOut)
                                .add("Vout", circuit.power.vOut)
                                .add("IL", circuit.power.ilOut)
                                .add("d", circuit.control.dutyOut)
                                .add("Error", circuit.control.errorOut)
                                .add("Cost", circuit.costCalculator.currentCost);
                        switch (event) {
                            case INPUT_DROP: {
                                var dropTime = Instant.of(design.switchingPeriod().value() * 3);
                                circuit.source.voltage.set(dropTime,
                                        design.inputVoltage.minus(1));
                                plot.add("VIn", circuit.source.out);
                            }
                                break;
                            case STEADY:
                                // NOP
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
