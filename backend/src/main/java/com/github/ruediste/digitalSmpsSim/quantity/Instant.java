package com.github.ruediste.digitalSmpsSim.quantity;

@HasUnit(Unit.Second)
public class Instant extends DoubleQuantity<Instant> {

    protected Instant(double value) {
        super(value);
    }

    @Override
    protected Instant self(double value) {
        return new Instant(value);
    }

    public static Instant of(double value) {
        return new Instant(value);
    }

    public Instant add(Duration duration) {
        return add(duration.value());
    }
}
