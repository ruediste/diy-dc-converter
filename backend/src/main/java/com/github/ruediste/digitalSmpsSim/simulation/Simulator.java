package com.github.ruediste.digitalSmpsSim.simulation;

import java.util.List;
import java.util.stream.Collectors;

import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.SiPrefix;

public class Simulator {

    public void simulate(Circuit circuit, double finalTime, Plot... plots) {
        simulate(circuit, finalTime, List.of(plots));
    }

    private void fillPlots(Instant time, List<Plot> plots, boolean addPoint) {
        for (var plot : plots) {
            if (plot.start != null && time.value() < plot.start.value())
                continue;
            if (plot.end != null && time.value() > plot.end.value())
                continue;
            for (var series : plot.series) {
                series.count++;
                series.sum += series.valueSupplier.get();
            }
            if (addPoint) {
                var values = new Plot.PlotValues();
                values.time = time;
                for (var series : plot.series) {
                    values.values.add(series.sum / series.count);
                    series.sum = 0;
                    series.count = 0;
                }
                plot.values.add(values);
            }
        }
    }

    public void simulate(Circuit circuit, double finalTime, List<Plot> plots) {
        circuit.initialize();
        circuit.elements.forEach(e -> e.initialize());
        circuit.propagateSignals();
        Instant time = Instant.of(0);
        fillPlots(time, plots, true);
        double plotPeriod = finalTime / 200;
        double nextPlot = plotPeriod;
        long stepCount = 0;
        while (time.value() < finalTime) {
            Instant stepEnd = null;
            Instant timeF = time;
            for (var element : circuit.elements) {
                var tmp = element.stepEndTime(time);
                if (tmp != null && tmp.value() > time.value()) {
                    if (stepEnd == null)
                        stepEnd = tmp;
                    else
                        stepEnd = stepEnd.min(tmp);
                }
            }
            if (stepEnd == null) {
                throw new RuntimeException("No Step End found at time " + SiPrefix.format(time.value(),
                        "s. End Times: " +
                                circuit.elements.stream().map(e -> e + ":" + e.stepEndTime(timeF))
                                        .collect(Collectors.joining(", "))));
            }

            if (stepCount == 0)
                stepEnd = Instant.of(1e-10);

            for (var element : circuit.elements)
                element.run(time, stepEnd, Duration.between(time, stepEnd));

            circuit.propagateSignals();
            time = stepEnd;
            {
                boolean addPoint = time.value() > nextPlot;
                fillPlots(stepEnd, plots, addPoint);
                if (addPoint) {
                    nextPlot = time.value() + plotPeriod;
                }
            }
            stepCount++;
        }

        plots.forEach(p -> p.finish());
    }
}
