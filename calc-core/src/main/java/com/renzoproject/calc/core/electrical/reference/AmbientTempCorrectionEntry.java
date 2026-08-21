package com.renzoproject.calc.core.electrical.reference;

/**
 * One row of PEC Table 3.10.2.6(B)(2)(a) — ambient temperature correction factors (30C base),
 * one factor per conductor temperature rating.
 *
 * @param ambientTempLowC  nullable — {@code null} for the open-ended "10 or less" row
 * @param factor60C        nullable — some ambient ranges have no published factor for a given
 *                          temperature rating (e.g. too hot for a 60C conductor to be valid at all)
 */
public record AmbientTempCorrectionEntry(
		String ambientTempRangeLabel,
		Double ambientTempLowC,
		double ambientTempHighC,
		Double factor60C,
		Double factor75C,
		Double factor90C) {

}
