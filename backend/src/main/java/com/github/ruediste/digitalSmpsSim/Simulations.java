package com.github.ruediste.digitalSmpsSim;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.ruediste.digitalSmpsSim.boost.BoostCircuit;
import com.github.ruediste.digitalSmpsSim.boost.BoostDesign;
import com.github.ruediste.digitalSmpsSim.simulation.Plot;
import com.github.ruediste.digitalSmpsSim.simulation.Simulator;

import jakarta.annotation.PostConstruct;

@Component
public class Simulations {

    Logger log = LoggerFactory.getLogger(Simulations.class);

    List<Plot> plots = new ArrayList<>();

    @PostConstruct
    public void run() {
        log.info("Running Simulations..");
        var design = new BoostDesign();
        log.info("Design: {}", design);

        var sim = new Simulator();
        var circuit = new BoostCircuit();

        design.applyTo(circuit);

        sim.simulate(circuit, circuit.switchingPeriod() * 200, plot()
                // .add("switch", circuit.control.switchOut)
                .add("Vout", circuit.power.vOut)
                .add("load", circuit.load.current)
                .add("IL", circuit.power.ilOut)

                , plot().add("switch", circuit.control.switchOut));
        log.info("Simulations complete");
    }

    private Plot plot() {
        var plot = new Plot();
        plots.add(plot);
        return plot;
    }
}
