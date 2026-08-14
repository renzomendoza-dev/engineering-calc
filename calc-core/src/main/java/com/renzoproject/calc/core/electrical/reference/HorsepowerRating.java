package com.renzoproject.calc.core.electrical.reference;

/**
 * Canonical motor horsepower rating identity, matching a PEC Article 4.30 table's size
 * notation exactly (e.g. {@code "1/4"}, {@code "1 1/2"}, {@code "200"}).
 *
 * <p>Deliberately generic (no motor-type-specific fields), similar in spirit to
 * {@link ConductorSize}, so it can be reused wherever motor HP sizes need a canonical,
 * sortable identity — e.g. a future "list known sizes" listing.
 *
 * @param label   size label as published in PEC tables
 * @param hpValue parsed numeric horsepower value
 */
public record HorsepowerRating(String label, double hpValue) {

	public static HorsepowerRating fromLabel(String label) {
		return new HorsepowerRating(label, parseHp(label));
	}

	/**
	 * Parses a PEC horsepower label into its numeric value. Handles plain whole numbers
	 * ({@code "200"} -> {@code 200.0}), plain fractions ({@code "1/4"} -> {@code 0.25}), and
	 * mixed numbers ({@code "1 1/2"} -> {@code 1.5}, {@code "7 1/2"} -> {@code 7.5}) — these
	 * labels aren't directly sortable/comparable as plain numbers without this conversion.
	 */
	public static double parseHp(String label) {
		String trimmed = label.trim();
		String[] parts = trimmed.split("\\s+");
		if (parts.length == 2) {
			return Double.parseDouble(parts[0]) + parseFraction(parts[1]);
		}
		if (parts[0].contains("/")) {
			return parseFraction(parts[0]);
		}
		return Double.parseDouble(parts[0]);
	}

	private static double parseFraction(String fraction) {
		String[] numeratorDenominator = fraction.split("/");
		return Double.parseDouble(numeratorDenominator[0]) / Double.parseDouble(numeratorDenominator[1]);
	}

}
