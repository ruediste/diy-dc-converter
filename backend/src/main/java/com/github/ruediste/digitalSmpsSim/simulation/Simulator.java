package com.github.ruediste.digitalSmpsSim.simulation;

import java.util.List;

import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.SiPrefix;

public class Simulator {

    public void simulate(Circuit circuit, double finalTime, Plot... plots) {
        simulate(circuit, finalTime, List.of(plots));
    }

    private void fillPlots(Instant time, List<Plot> plots) {
        for (var plot : plots) {
            if (plot.start != null && time.value() < plot.start.value())
                continue;
            if (plot.end != null && time.value() > plot.end.value())
                continue;
            var values = new Plot.PlotValues();
            values.time = time;
            for (var series : plot.series) {
                values.values.add(series.valueSupplier.get());
            }
            plot.values.add(values);
        }
    }

    public void simulate(Circuit circuit, double finalTime, List<Plot> plots) {
        circuit.initialize();
        circuit.elements.forEach(e -> e.initialize());
        circuit.propagateSignals();
        Instant time = Instant.of(0);
        fillPlots(time, plots);
        long stepCount = 0;
        while (time.value() < finalTime) {
            Instant stepEnd = null;
            for (var element : circuit.elements) {
                var tmp = element.stepEndTime(time);
                if (tmp != null && tmp.value() > time.value()) {
                    if (stepEnd == null)
                        stepEnd = tmp;
                    else
                        stepEnd = stepEnd.min(tmp);
                }
            }
            if (stepEnd == null)
                throw new RuntimeException("No Step End found at time " + SiPrefix.format(time.value(), "s"));

            if (stepCount == 0)
                stepEnd = Instant.of(1e-10);

            for (var element : circuit.elements)
                element.run(time, stepEnd, Duration.between(time, stepEnd));

            circuit.propagateSignals();
            time = stepEnd;
            fillPlots(stepEnd, plots);
            stepCount++;
        }

        plots.forEach(p -> p.finish());
    }
}
