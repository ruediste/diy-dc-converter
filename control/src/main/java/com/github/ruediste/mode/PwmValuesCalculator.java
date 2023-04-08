package com.github.ruediste.mode;

public class PwmValuesCalculator {

    public static class PwmValues {
        public long reload;
        public long compare;
        public long prescale;
    }

    public double clock = 84e6;
    public long bits = 16;

    public PwmValues calculate(double frequency, double duty) {
        var values = new PwmValues();
        values.prescale = ((long) Math.ceil(clock / ((1 << bits) * frequency))) - 1;
        values.reload = Math.round(clock / ((values.prescale + 1) * frequency));
        values.compare = Math.round(values.reload * duty);
        return values;
    }
}
