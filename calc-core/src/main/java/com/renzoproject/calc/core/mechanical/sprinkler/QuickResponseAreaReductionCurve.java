package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * NFPA 13's design area reduction for quick-response sprinklers, as a function of ceiling height
 * (Figure 19.3.3.2.3.1 in the 2019 edition).
 *
 * <p>Piecewise, exactly as the figure states it: a flat reduction below {@code lowCeilingFt}, a
 * straight line {@code y = slope * x + intercept} from {@code lowCeilingFt} to
 * {@code highCeilingFt} <b>inclusive</b>, and none above. The line is stored by its printed
 * coefficients rather than as two end points so it stays the figure's own equation.
 *
 * <p>The step at {@code highCeilingFt} is real, not a rounding artefact: 20.0 ft takes the
 * line's final value (25 percent) and anything above takes none.
 *
 * @param confidence          how the curve itself was checked: {@code verified} means against the
 *                            printed figure
 * @param conditionsConfidence how the <i>conditions of use</i> were checked. Distinct from
 *                            {@code confidence} on purpose: the curve can be exact while the text
 *                            that says when it may be used has not been transcribed
 * @param conditionsNote      what those conditions are believed to cover and that they are not checked
 */
public record QuickResponseAreaReductionCurve(
		String standard,
		String confidence,
		String conditionsConfidence,
		String conditionsNote,
		double lowCeilingFt,
		double highCeilingFt,
		double reductionBelowLowCeilingPercent,
		double reductionAboveHighCeilingPercent,
		double slopePercentPerFt,
		double interceptPercent) {

	public QuickResponseAreaReductionCurve {
		if (lowCeilingFt >= highCeilingFt) {
			throw new CalculationException("area reduction curve is malformed: lowCeilingFt (" + lowCeilingFt
					+ ") must be below highCeilingFt (" + highCeilingFt + ")");
		}
	}

	/**
	 * Percent reduction to the design area at this ceiling height.
	 *
	 * @throws CalculationException if the height is not positive
	 */
	public double reductionPercentAt(double ceilingHeightFt) {
		if (ceilingHeightFt <= 0) {
			throw new CalculationException("ceilingHeightFt must be positive (was " + ceilingHeightFt + ")");
		}
		if (ceilingHeightFt < lowCeilingFt) {
			return reductionBelowLowCeilingPercent;
		}
		if (ceilingHeightFt > highCeilingFt) {
			return reductionAboveHighCeilingPercent;
		}
		return slopePercentPerFt * ceilingHeightFt + interceptPercent;
	}

}
