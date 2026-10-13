package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * HTTP request body for working out a sprinkler remote area. <b>Units:</b> ft2, feet, gpm/ft2.
 *
 * <p>Meant to be called as a form fills in: the first five figures are enough for the area, the
 * flow per sprinkler and the first sprinkler's pressure, and {@code sprinklerSpacingFt} is only
 * needed for the layout. The rule that a ceiling height is needed when quick-response sprinklers
 * are stated is enforced by calc-core's {@code RemoteAreaPlanInput}, not duplicated here.
 *
 * @param designAreaSqFt               the area of sprinkler operation before any reduction
 * @param densityGpmPerSqFt            design density
 * @param kFactor                      K of the sprinklers in the remote area
 * @param coveragePerSprinklerSqFt     the area each sprinkler protects, applied to all of them
 * @param sprinklerSpacingFt           distance between sprinklers along a branch line; optional --
 *                                     without it the layout fields come back null
 * @param quickResponseSprinklers      whether quick-response sprinklers are installed
 * @param ceilingHeightFt              ceiling height; required when quick-response is true
 * @param maxCoveragePerSprinklerSqFt  largest area one sprinkler may protect for this hazard; if
 *                                     given, the coverage is checked against it
 * @param listedMinimumPressurePsi     <b>optional</b>. The minimum operating pressure the sprinkler's
 *                                     listing specifies (NFPA 13 27.2.4.11.2). It can only raise the
 *                                     7 psi the standard sets for any sprinkler, never lower it
 */
public record RemoteAreaPlanRequest(
		@NotNull @Positive Double designAreaSqFt,
		@NotNull @Positive Double densityGpmPerSqFt,
		@NotNull @Positive Double kFactor,
		@NotNull @Positive Double coveragePerSprinklerSqFt,
		@Positive Double sprinklerSpacingFt,
		@NotNull Boolean quickResponseSprinklers,
		@Positive Double ceilingHeightFt,
		@Positive Double maxCoveragePerSprinklerSqFt,
		@PositiveOrZero Double listedMinimumPressurePsi) {

}
