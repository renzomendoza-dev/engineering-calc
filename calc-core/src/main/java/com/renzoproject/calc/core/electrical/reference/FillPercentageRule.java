package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * PEC Table 10.1.1.1 — maximum allowed conduit fill percentage by conductor count. Only three
 * values, so hardcoded rather than JSON-backed.
 */
public final class FillPercentageRule {

	public static final double ONE_CONDUCTOR_PERCENT = 53.0;
	public static final double TWO_CONDUCTORS_PERCENT = 31.0;
	public static final double OVER_TWO_CONDUCTORS_PERCENT = 40.0;

	private FillPercentageRule() {
	}

	/**
	 * @throws CalculationException if conductorCount is less than 1
	 */
	public static double allowedFillPercent(int conductorCount) {
		if (conductorCount < 1) {
			throw new CalculationException("conductorCount must be at least 1");
		}
		if (conductorCount == 1) {
			return ONE_CONDUCTOR_PERCENT;
		}
		if (conductorCount == 2) {
			return TWO_CONDUCTORS_PERCENT;
		}
		return OVER_TWO_CONDUCTORS_PERCENT;
	}

}
