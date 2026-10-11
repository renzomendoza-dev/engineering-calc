package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input for {@link QuickResponseAreaReductionCalculator}. <b>Units:</b> ft2 and feet -- see
 * {@code package-info.java}.
 *
 * @param designAreaSqFt           the design area before any reduction, normally the density/area
 *                                 curve point being designed to; must be positive
 * @param ceilingHeightFt          height of the ceiling the sprinklers are under; must be positive
 * @param quickResponseSprinklers  whether quick-response sprinklers are installed. {@code false}
 *                                 yields no reduction, because the figure only covers them
 * @throws CalculationException if a figure is not positive
 */
public record QuickResponseAreaReductionInput(
		double designAreaSqFt,
		double ceilingHeightFt,
		boolean quickResponseSprinklers) {

	public QuickResponseAreaReductionInput {
		if (designAreaSqFt <= 0) {
			throw new CalculationException("designAreaSqFt must be positive (was " + designAreaSqFt + ")");
		}
		if (ceilingHeightFt <= 0) {
			throw new CalculationException("ceilingHeightFt must be positive (was " + ceilingHeightFt + ")");
		}
	}

}
