package com.github.ruediste.digitalSmpsSim.quantity;

public enum SiPrefix {
	TERA("T", 1e12), GIGA("G", 1e9), MEGA("M", 1e6), //
	KILO("k", 1e3), ONE("", 1), MILLI("m", 1e-3), //
	MICRO("\u03bc", 1e-6), NANO("n", 1e-9), //
	PICO("p", 1e-12);

	public final String symbol;
	public final double multiplier;

	private SiPrefix(String symbol, double multiplier) {
		this.symbol = symbol;
		this.multiplier = multiplier;
	}

	public static SiPrefix get(double value) {
		for (SiPrefix p : values()) {
			if (p.multiplier <= value) {
				return p;
			}
		}
		return null;
	}

	public static String format(double value, String unit) {
		var prefix = get(value);
		if (prefix == null) {
			return "%.3f%s".formatted(value, unit);
		}
		return "%.3f%s%s".formatted(value / prefix.multiplier, prefix.symbol, unit);
	}
}
