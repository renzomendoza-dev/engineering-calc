package com.renzoproject.calc.core.electrical.generatorset;

import java.util.Locale;

/** Text helpers shared by the generator set calculator and its parts. */
final class GeneratorSetText {

	private GeneratorSetText() {
	}

	static String labelOf(String label, String fallback) {
		return label == null || label.isBlank() ? fallback : label.trim();
	}

	/** Whole numbers without a decimal point, anything else to one decimal place. */
	static String num(double value) {
		return Math.abs(value - Math.rint(value)) < 1e-9
				? String.format(Locale.ROOT, "%,.0f", value)
				: String.format(Locale.ROOT, "%,.1f", value);
	}

}
