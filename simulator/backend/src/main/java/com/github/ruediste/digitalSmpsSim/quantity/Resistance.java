package com.github.ruediste.digitalSmpsSim.quantity;

@HasUnit(Unit.Ohm)
public class Resistance extends DoubleQuantity<Resistance> {

    protected Resistance(double value) {
        super(value);
    }

    @Override
    protected Resistance self(double value) {
        return new Resistance(value);
    }

    public static Resistance of(double value) {
        return new Resistance(value);
    }
}
