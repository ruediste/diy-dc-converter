package com.github.ruediste.digitalSmpsSim.quantity;

@HasUnit(Unit.Ampere)
public class Current extends DoubleQuantity<Current> {

    protected Current(double value) {
        super(value);
    }

    public static Current of(double value) {
        return new Current(value);
    }

    @Override
    protected Current self(double value) {
        return of(value);
    }

}
