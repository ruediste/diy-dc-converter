package com.github.ruediste.digitalSmpsSim.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.ruediste.digitalSmpsSim.quantity.SiPrefix;
import com.github.ruediste.digitalSmpsSim.quantity.Unit;

public class Plot {
    public double samplePeriod;
    public Double start;
    public Double end;
    public String title;

    public SiPrefix timePrefix;

    public List<Series> series = new ArrayList<>();

    public List<PlotValues> values = new ArrayList<>();

    public List<PlotYAxis> axes = new ArrayList<>();

    public static class PlotYAxis {
        public int index;
        public Unit unit;
        public String unitSymbol;
        public boolean isRight;
    }

    public static class Series {
        public String name;
        public Unit unit;
        public Supplier<Double> valueSupplier;
        public boolean stepAfter;
        public int yAxisIndex;

        public double sum;
        public double sumDuration;

        public Series(String name, Unit unit, Supplier<Double> valueSupplier) {
            this.name = name;
            this.unit = unit;
            this.valueSupplier = valueSupplier;
        }
    }

    public static class PlotValues {
        public double time;
        public List<Double> values = new ArrayList<>();
    }

    public Plot(Circuit circuit, String title) {
        circuit.plots.add(this);
        this.title = title;
    }

    public <T> Plot add(String name, Unit unit, SimulationValue<Double> value) {
        return add(name, unit, value::get);
    }

    public <T> Plot add(String name, Unit unit, Supplier<Double> valueSupplier) {
        this.series.add(new Series(name, unit, valueSupplier));
        if (unit == Unit.Digital)
            stepAfter();
        return this;
    }

    public Plot start(Double seconds) {
        this.start = seconds;
        return this;
    }

    public Plot end(Double seconds) {
        this.end = seconds;
        return this;
    }

    private Series lastSeries() {
        return series.get(this.series.size() - 1);
    }

    public Plot stepAfter() {
        lastSeries().stepAfter = true;
        return this;
    }

    public void finish() {
        if (false) {
            var units = series.stream().map(x -> x.unit).distinct().sorted().toList();
            axes = units.stream().map(unit -> {
                var axis = new PlotYAxis();
                axis.unit = unit;
                axis.unitSymbol = unit.symbol;
                return axis;
            }).toList();
            for (int i = 0; i < axes.size(); i++) {
                var axis = axes.get(i);
                axis.index = i;
            }
            var axisByUnit = axes.stream().collect(Collectors.toMap(x -> x.unit, x -> x));
            for (var s : series) {
                s.yAxisIndex = axisByUnit.get(s.unit).index;
                s.name += "[" + s.unit.symbol + "]";
            }
        } else {
            for (int i = 0; i < series.size(); i++) {
                var s = series.get(i);
                var axis = new PlotYAxis();
                axis.unit = s.unit;
                axis.unitSymbol = s.name + "[" + s.unit.symbol + "]";
                axis.index = i;
                s.yAxisIndex = i;
                axes.add(axis);
            }
        }

        axes.stream().skip(axes.size() / 2).forEach(a -> a.isRight = true);

        if (!values.isEmpty()) {
            if (start == null) {
                start = values.get(0).time;
            }
            if (end == null) {
                end = values.get(values.size() - 1).time;
            }
            timePrefix = SiPrefix.get(end);
        } else {
            start = 0.;
            end = 1.;
            timePrefix = SiPrefix.ONE;
        }
    }

}
