package com.github.ruediste.digitalSmpsSim.quantity;

@HasUnit(Unit.Second)
public class Duration extends DoubleQuantity<Duration> {

    protected Duration(double value) {
        super(value);
    }

    @Override
    protected Duration self(double value) {
        return new Duration(value);
    }

    public static Duration of(double value) {
        return new Duration(value);
    }

    public static Duration between(Instant start, Instant end) {
        return of(end.value() - start.value());
    }

}
