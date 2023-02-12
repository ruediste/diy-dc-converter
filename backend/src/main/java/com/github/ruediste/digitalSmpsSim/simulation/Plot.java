package com.github.ruediste.digitalSmpsSim.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.ruediste.digitalSmpsSim.PlotRest.PlotGroup;
import com.github.ruediste.digitalSmpsSim.quantity.Duration;
import com.github.ruediste.digitalSmpsSim.quantity.HasUnit;
import com.github.ruediste.digitalSmpsSim.quantity.Instant;
import com.github.ruediste.digitalSmpsSim.quantity.SiPrefix;
import com.github.ruediste.digitalSmpsSim.quantity.SiPrefixRest;
import com.github.ruediste.digitalSmpsSim.quantity.SiPrefixRest.SiPrefixPMod;
import com.github.ruediste.digitalSmpsSim.quantity.Unit;
import com.google.common.reflect.TypeToken;

public class Plot {
    public Duration samplePeriod;
    public Instant start;
    public Instant end;
    public String title;

    public SiPrefixPMod timePrefix;

    public List<Series> series = new ArrayList<>();

    public List<PlotValues> values = new ArrayList<>();

    public List<PlotYAxis> axes = new ArrayList<>();
    private PlotGroup plotGroup;

    public static class PlotYAxis {
        public int index;
        public Unit unit;
        public String unitSymbol;
        public boolean isRight;
    }

    public static class Series {
        public String name;
        public Unit unit;
        public String unitSymbol;
        @JsonIgnore
        public Supplier<Double> valueSupplier;
        public boolean stepAfter;
        public int yAxisIndex;

        public double sum;
        public long count;

        public Series(String name, Unit unit, Supplier<Double> valueSupplier) {
            this.name = name;
            this.unit = unit;
            this.valueSupplier = valueSupplier;
        }
    }

    public static class PlotValues {
        public Instant time;
        public List<Double> values = new ArrayList<>();
    }

    public Plot(PlotGroup plotGroup) {
        this.plotGroup = plotGroup;
    }

    public <T> Plot add(String name, Plottable plottable) {
        Class<?> quantityCls = null;
        if (plottable instanceof ElementOutput) {
            quantityCls = TypeToken.of(plottable.getClass()).resolveType(ElementOutput.class.getTypeParameters()[0])
                    .getRawType();
        } else if (plottable instanceof ElementInput) {
            quantityCls = TypeToken.of(plottable.getClass()).resolveType(ElementInput.class.getTypeParameters()[0])
                    .getRawType();
        } else if (plottable.getClass().isAnnotationPresent(HasUnit.class))
            quantityCls = plottable.getClass();

        Unit unit = null;
        if (quantityCls != null) {
            var hasUnit = quantityCls.getAnnotation(HasUnit.class);
            unit = hasUnit == null ? null : hasUnit.value();
        }
        if (unit == null) {
            throw new RuntimeException("No quantity found for " + plottable + " quantityCls " + quantityCls);
        }
        this.series.add(new Series(name, unit, () -> plottable.plotValue()));
        if (unit == Unit.Digital)
            stepAfter();
        return this;
    }

    public Plot start(double seconds) {
        this.start = Instant.of(seconds);
        return this;
    }

    public Plot end(double seconds) {
        this.end = Instant.of(seconds);
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
        }
        axes.stream().skip(axes.size() / 2).forEach(a -> a.isRight = true);

        series.forEach(s -> {
            s.unitSymbol = s.unit.symbol;
        });
        if (!values.isEmpty()) {
            if (start == null) {
                start = values.get(0).time;
            }
            if (end == null) {
                end = values.get(values.size() - 1).time;
            }
            timePrefix = SiPrefixRest.toPMod(SiPrefix.get(end.value()));
        } else {
            start = Instant.of(0);
            end = Instant.of(1);
            timePrefix = SiPrefixRest.toPMod(SiPrefix.ONE);
        }
        this.plotGroup.plots.add(this);
    }

}
