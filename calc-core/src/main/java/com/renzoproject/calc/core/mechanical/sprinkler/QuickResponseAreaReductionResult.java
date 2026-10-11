package com.renzoproject.calc.core.mechanical.sprinkler;

import java.util.List;

/**
 * Result of {@link QuickResponseAreaReductionCalculator}. <b>Units:</b> ft2, feet, percent.
 *
 * @param baseDesignAreaSqFt        the area as given, before any reduction
 * @param ceilingHeightFt           the ceiling height the curve was read at
 * @param quickResponseSprinklers   as given
 * @param reductionPercent          percent taken off the design area; {@code 0} when none applies
 * @param adjustedDesignAreaSqFt    {@code baseDesignAreaSqFt * (1 - reductionPercent / 100)}
 * @param reductionApplied          true only when quick-response sprinklers were stated AND the
 *                                  ceiling height earns a non-zero reduction. Lets a caller tell
 *                                  "no reduction because none was due" from "no reduction because
 *                                  the area is unchanged"
 * @param source                    the figure the curve is from, for display and citation
 * @param confidence                how the curve was checked ({@code verified} = against the
 *                                  printed figure)
 * @param notes                     what happened and what was not checked, in plain language --
 *                                  always read alongside the figures, never a substitute for them
 */
public record QuickResponseAreaReductionResult(
		double baseDesignAreaSqFt,
		double ceilingHeightFt,
		boolean quickResponseSprinklers,
		double reductionPercent,
		double adjustedDesignAreaSqFt,
		boolean reductionApplied,
		String source,
		String confidence,
		List<String> notes) {

}
