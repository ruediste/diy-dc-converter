package com.github.ruediste.digitalSmpsSim.quantity;

@HasUnit(Unit.Digital)
public class DigitalValue extends Quantity {
    private final boolean value;

    public static final DigitalValue HIGH = new DigitalValue(true);
    public static final DigitalValue LOW = new DigitalValue(false);

    private DigitalValue(boolean value) {
        this.value = value;
    }

    public boolean value() {
        return value;
    }

    public static DigitalValue of(boolean value) {
        if (value)
            return HIGH;
        return LOW;
    }

    public boolean isHigh() {
        return value;
    }

    public boolean isLow() {
        return !value;
    }

    @Override
    public double plotValue() {
        return value ? 1 : 0;
    }
}
