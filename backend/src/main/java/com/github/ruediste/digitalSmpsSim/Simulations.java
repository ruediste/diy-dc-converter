package com.github.ruediste.digitalSmpsSim;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.ruediste.digitalSmpsSim.boost.BoostDesign;
import com.github.ruediste.digitalSmpsSim.quantity.Current;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.Voltage;
import com.github.ruediste.digitalSmpsSim.simulation.Plot;
import com.github.ruediste.digitalSmpsSim.simulation.Simulator;

import jakarta.annotation.PostConstruct;

@Component
public class Simulations {

    Logger log = LoggerFactory.getLogger(Simulations.class);

    List<Plot> plots = new ArrayList<>();

    private enum Event {
        STEADY,
        INPUT_DROP
    }

    @PostConstruct
    public void run() {
        log.info("Running Simulations..");
        var design = new BoostDesign();
        design.outputRipple = Voltage.of(0.1);
        log.info("Design:\n{}", design);

        List<Voltage> vOuts = List.of(6., 10., 20., 40.).stream().map(v -> Voltage.of(v)).toList();
        List<Current> iOuts = List.of(0.01, 0.1, 1., 2.).stream().map(v -> Current.of(v)).toList();

        var sim = new Simulator();
        for (var event : Event.values())
            for (var vOut : vOuts)
                for (var iOut : iOuts) {
                    var circuit = design.circuit();
                    circuit.load.resistance = vOut.divide(iOut);
                    circuit.power.vCap = vOut;

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

                    var plot = plot(event + " " + vOut + " - " + iOut)
                            .add("Vout", circuit.power.vOut)
                            .add("IL", circuit.power.ilOut);
                    switch (event) {
                        case INPUT_DROP:
                            circuit.source.voltage.set(Instant.of(design.switchingPeriod().value() * 3),
                                    design.inputVoltage.minus(1));
                            plot.add("VIn", circuit.source.out);
                            break;
                        case STEADY:
                            // NOP
                            break;
                        default:
                            break;

                    }

                    sim.simulate(circuit, circuit.switchingPeriod() * 80,
                            plot);

                }

        log.info("Simulations complete");
    }

    private Plot plot(String title) {
        var plot = new Plot();
        plot.title = title;
        plots.add(plot);
        return plot;
    }
}
