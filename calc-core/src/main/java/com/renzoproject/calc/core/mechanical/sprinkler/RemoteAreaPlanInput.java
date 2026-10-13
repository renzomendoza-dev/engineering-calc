package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input for {@link RemoteAreaPlanCalculator}. <b>Units:</b> ft2, feet, gpm/ft2 -- see
 * {@code package-info.java}.
 *
 * <p>Built so the plan can be worked out in stages as a form is filled in. The first four figures
 * are enough for the area, the flow per sprinkler and the first sprinkler's pressure; the spacing
 * along the branch is only needed for the layout, and may be omitted until it is known.
 *
 * @param designAreaSqFt            the area of sprinkler operation before any reduction, normally
 *                                  the density/area curve point being designed to; must be positive
 * @param densityGpmPerSqFt         design density; must be positive
 * @param kFactor                   the K of the sprinklers in the remote area; must be positive
 * @param coveragePerSprinklerSqFt  the area each sprinkler protects, ft2, applied to every sprinkler
 *                                  in the remote area; must be positive
 * @param sprinklerSpacingFt        distance between sprinklers ALONG a branch line, ft. <b>May be
 *                                  null</b>: the layout (sprinklers per branch, branch lines) is then
 *                                  not worked out, and everything that doesn't need it still is.
 *                                  The spacing between branch lines follows from the coverage
 * @param quickResponseSprinklers   whether quick-response sprinklers are installed, which is what
 *                                  allows the ceiling-height area reduction
 * @param ceilingHeightFt           ceiling height, ft. Required (and positive) when
 *                                  {@code quickResponseSprinklers} is true, since it is what the
 *                                  reduction is read against; ignored otherwise
 * @param maxCoveragePerSprinklerSqFt the largest area one sprinkler may protect for this hazard and
 *                                  construction, ft2; <b>may be null</b>, in which case coverage is
 *                                  not checked against it
 * @param listedMinimumPressurePsi  the minimum operating pressure the sprinkler's listing specifies
 *                                  (NFPA 13 27.2.4.11.2), psi; <b>may be null</b>. It can only raise
 *                                  the floor above the standard's own, never lower it
 * @throws CalculationException if a rule above is violated
 */
public record RemoteAreaPlanInput(
		double designAreaSqFt,
		double densityGpmPerSqFt,
		double kFactor,
		double coveragePerSprinklerSqFt,
		Double sprinklerSpacingFt,
		boolean quickResponseSprinklers,
		Double ceilingHeightFt,
		Double maxCoveragePerSprinklerSqFt,
		Double listedMinimumPressurePsi) {

	/** A plan with no listing-specific minimum pressure. */
	public RemoteAreaPlanInput(
			double designAreaSqFt,
			double densityGpmPerSqFt,
			double kFactor,
			double coveragePerSprinklerSqFt,
			Double sprinklerSpacingFt,
			boolean quickResponseSprinklers,
			Double ceilingHeightFt,
			Double maxCoveragePerSprinklerSqFt) {
		this(designAreaSqFt, densityGpmPerSqFt, kFactor, coveragePerSprinklerSqFt, sprinklerSpacingFt,
				quickResponseSprinklers, ceilingHeightFt, maxCoveragePerSprinklerSqFt, null);
	}

	public RemoteAreaPlanInput {
		requirePositive(designAreaSqFt, "designAreaSqFt");
		requirePositive(densityGpmPerSqFt, "densityGpmPerSqFt");
		requirePositive(kFactor, "kFactor");
		requirePositive(coveragePerSprinklerSqFt, "coveragePerSprinklerSqFt");
		if (sprinklerSpacingFt != null && sprinklerSpacingFt <= 0) {
			throw new CalculationException("sprinklerSpacingFt must be positive when given (was "
					+ sprinklerSpacingFt + ")");
		}
		if (quickResponseSprinklers && (ceilingHeightFt == null || ceilingHeightFt <= 0)) {
			throw new CalculationException(
					"ceilingHeightFt is required, and must be positive, when quickResponseSprinklers is true -- "
							+ "the reduction is read against it");
		}
		if (maxCoveragePerSprinklerSqFt != null && maxCoveragePerSprinklerSqFt <= 0) {
			throw new CalculationException("maxCoveragePerSprinklerSqFt must be positive when given");
		}
		if (listedMinimumPressurePsi != null && listedMinimumPressurePsi < 0) {
			throw new CalculationException("listedMinimumPressurePsi must not be negative when given (was "
					+ listedMinimumPressurePsi + ")");
		}
	}

	private static void requirePositive(double value, String name) {
		if (value <= 0) {
			throw new CalculationException(name + " must be positive (was " + value + ")");
		}
	}

}
