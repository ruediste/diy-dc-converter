package com.github.ruediste.digitalSmpsSim.quantity;

@HasUnit(Unit.Volt)
public class Voltage extends DoubleQuantity<Voltage> {

    protected Voltage(double value) {
        super(value);
    }

    public static Voltage of(double value) {
        return new Voltage(value);
    }

    @Override
    protected Voltage self(double value) {
        return of(value);
    }

    public Resistance divide(Current current) {
        return Resistance.of(value() / current.value());
    }

    public Current divide(Resistance resistance) {
        return Current.of(value() / resistance.value());
    }
}
