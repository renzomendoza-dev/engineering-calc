package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input for {@link RemoteAreaPlanCalculator}. <b>Units:</b> ft2, feet, gpm/ft2 -- see
 * {@code package-info.java}.
 *
 * @param designAreaSqFt            the area of sprinkler operation before any reduction, normally
 *                                  the density/area curve point being designed to; must be positive
 * @param densityGpmPerSqFt         design density; must be positive
 * @param kFactor                   the K of the sprinklers in the remote area; must be positive
 * @param sprinklerSpacingFt        distance between sprinklers ALONG a branch line; must be positive
 * @param branchSpacingFt           distance BETWEEN branch lines; must be positive
 * @param quickResponseSprinklers   whether quick-response sprinklers are installed, which is what
 *                                  allows the ceiling-height area reduction
 * @param ceilingHeightFt           ceiling height, ft. Required (and positive) when
 *                                  {@code quickResponseSprinklers} is true, since it is what the
 *                                  reduction is read against; ignored otherwise
 * @param maxCoveragePerSprinklerSqFt the largest area one sprinkler may protect for this hazard and
 *                                  construction, ft2; <b>may be null</b>, in which case spacing is
 *                                  not checked against it
 * @throws CalculationException if a rule above is violated
 */
public record RemoteAreaPlanInput(
		double designAreaSqFt,
		double densityGpmPerSqFt,
		double kFactor,
		double sprinklerSpacingFt,
		double branchSpacingFt,
		boolean quickResponseSprinklers,
		Double ceilingHeightFt,
		Double maxCoveragePerSprinklerSqFt) {

	public RemoteAreaPlanInput {
		requirePositive(designAreaSqFt, "designAreaSqFt");
		requirePositive(densityGpmPerSqFt, "densityGpmPerSqFt");
		requirePositive(kFactor, "kFactor");
		requirePositive(sprinklerSpacingFt, "sprinklerSpacingFt");
		requirePositive(branchSpacingFt, "branchSpacingFt");
		if (quickResponseSprinklers && (ceilingHeightFt == null || ceilingHeightFt <= 0)) {
			throw new CalculationException(
					"ceilingHeightFt is required, and must be positive, when quickResponseSprinklers is true -- "
							+ "the reduction is read against it");
		}
		if (maxCoveragePerSprinklerSqFt != null && maxCoveragePerSprinklerSqFt <= 0) {
			throw new CalculationException("maxCoveragePerSprinklerSqFt must be positive when given");
		}
	}

	private static void requirePositive(double value, String name) {
		if (value <= 0) {
			throw new CalculationException(name + " must be positive (was " + value + ")");
		}
	}

}
