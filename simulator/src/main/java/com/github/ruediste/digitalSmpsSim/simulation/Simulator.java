package com.github.ruediste.digitalSmpsSim.simulation;

import java.util.List;
import java.util.stream.Collectors;

import com.github.ruediste.digitalSmpsSim.quantity.SiPrefix;

public class Simulator {

    public void simulate(Circuit circuit, double finalTime, Plot... plots) {
        simulate(circuit, finalTime, List.of(plots));
    }

    private void fillPlots(double time, double stepDuration, List<Plot> plots, boolean addPoint) {
        for (var plot : plots) {
            if (plot.start != null && time < plot.start)
                continue;
            if (plot.end != null && time > plot.end)
                continue;
            for (var series : plot.series) {
                series.count += stepDuration;
                series.sum += series.valueSupplier.get() * stepDuration;
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
        circuit.elements.forEach(e -> e.postInitialize());
        circuit.propagateSignals();
        double time = 0;
        fillPlots(time, 1, plots, true);
        double plotPeriod = finalTime / 200;
        double nextPlot = plotPeriod;
        long stepCount = 0;
        while (time < finalTime) {
            Double stepEnd = null;
            double timeF = time;
            for (var element : circuit.elements) {
                var tmp = element.stepEndTime(time);
                if (tmp != null && tmp > time) {
                    if (stepEnd == null)
                        stepEnd = tmp;
                    else
                        stepEnd = Math.min(stepEnd, tmp);
                }
            }
            if (stepEnd == null) {
                throw new RuntimeException("No Step End found at time " + SiPrefix.format(time,
                        "s. End Times: " +
                                circuit.elements.stream().map(e -> e + ":" + e.stepEndTime(timeF))
                                        .collect(Collectors.joining(", "))));
            }

            if (stepCount == 0)
                stepEnd = 1e-10;

            double stepDuration = stepEnd - time;
            for (var element : circuit.elements)
                element.run(time, stepEnd, stepDuration);

            circuit.propagateSignals();
            circuit.withUpdatedValues.forEach(x -> x.run());
            circuit.withUpdatedValues.clear();
            time = stepEnd;
            {
                boolean addPoint = time > nextPlot;
                fillPlots(stepEnd, stepDuration, plots, addPoint);
                if (addPoint) {
                    nextPlot += plotPeriod;
                }
            }
            stepCount++;
        }

        circuit.elements.forEach(e -> e.finish());
        plots.forEach(p -> p.finish());
    }
}
