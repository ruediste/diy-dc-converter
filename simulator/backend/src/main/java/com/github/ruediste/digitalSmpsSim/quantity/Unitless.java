package com.github.ruediste.digitalSmpsSim.quantity;

@HasUnit(Unit.Number)
public class Unitless extends DoubleQuantity<Unitless> {

    protected Unitless(double value) {
        super(value);
    }

    @Override
    protected Unitless self(double value) {
        return new Unitless(value);
    }

    public static Unitless of(double value) {
        return new Unitless(value);
    }
}
