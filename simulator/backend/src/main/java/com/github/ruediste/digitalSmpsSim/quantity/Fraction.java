package com.github.ruediste.digitalSmpsSim.quantity;

@HasUnit(Unit.Number)
public class Fraction extends DoubleQuantity<Fraction> {

    protected Fraction(double value) {
        super(value);
    }

    @Override
    protected Fraction self(double value) {
        return new Fraction(value);
    }

    public static Fraction of(double value) {
        return new Fraction(value);
    }
}
