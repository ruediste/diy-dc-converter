package com.github.ruediste.digitalSmpsSim.quantity;

public enum Unit {
    Ampere("A"),
    Volt("V"),
    Second("s"),
    Digital("D"),
    Ohm("\u03A9"),
    Farad("F"),
    Hertz("Hz"),
    Number("1");

    public final String symbol;

    Unit(String symbol) {
        this.symbol = symbol;

    }
}
