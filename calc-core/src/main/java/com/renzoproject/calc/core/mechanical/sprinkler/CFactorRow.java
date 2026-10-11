package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * One row of {@code reference/sprinkler/hazen-williams-c.json}, as published.
 *
 * @param pipeType          pipe type key, e.g. {@code STEEL}, {@code COPPER}, {@code CPVC}
 * @param systemType        {@code WET}/{@code DRY}/{@code PREACTION}, or {@code ANY} when the
 *                          value does not vary with service type
 * @param cFactor           Hazen-Williams C
 * @param epsilonFactorInch equivalent absolute roughness NFPA 13's Table A.27.2.4.8.2 pairs with
 *                          this C value, inches; {@code null} where the annex doesn't list it.
 *                          Reference only -- Hazen-Williams takes C, not roughness, so nothing in
 *                          calc-core reads this
 * @param epsilonFactorMm   the same value in millimetres, as published
 * @param confidence        {@code verified}, {@code verify} or {@code placeholder}
 */
public record CFactorRow(
		String pipeType,
		String systemType,
		double cFactor,
		Double epsilonFactorInch,
		Double epsilonFactorMm,
		String label,
		String confidence,
		String sourceNote) {

	/** Marker used in the reference file for a C factor that doesn't depend on service type. */
	public static final String ANY_SYSTEM_TYPE = "ANY";

}
