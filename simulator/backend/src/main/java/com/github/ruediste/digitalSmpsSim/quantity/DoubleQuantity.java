package com.github.ruediste.digitalSmpsSim.quantity;

public abstract class DoubleQuantity<TSelf extends DoubleQuantity<TSelf>> extends Quantity {
    private final double value;

    @Override
    public double plotValue() {
        return value();
    }

    public double value() {
        return value;
    }

    protected DoubleQuantity(double value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    protected TSelf self() {
        return (TSelf) this;
    }

    protected abstract TSelf self(double value);

    public TSelf add(TSelf other) {
        return add(other.value());
    }

    public TSelf add(double other) {
        return self(value + other);
    }

    public TSelf scale(double factor) {
        return self(value * factor);
    }

    public TSelf minus(TSelf other) {
        return minus(other.value());
    }

    public TSelf minus(double other) {
        return self(value - other);
    }

    public TSelf max(double other) {
        if (other > value)
            return self(other);
        return self();
    }

    public TSelf min(double other) {
        if (other < value)
            return self(other);
        return self();
    }

    public TSelf min(TSelf other) {
        return min(other.value());
    }

    @Override
    public String toString() {
        return SiPrefix.format(value, getClass().getAnnotation(HasUnit.class).value().symbol);
    }

    public int compareTo(TSelf other) {
        return Double.compare(value, other.value());
    }
}
