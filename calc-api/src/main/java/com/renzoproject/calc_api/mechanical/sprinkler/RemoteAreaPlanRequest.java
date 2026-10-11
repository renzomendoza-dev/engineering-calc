package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for laying out a sprinkler remote area. <b>Units:</b> ft2, feet, gpm/ft2.
 *
 * <p>The rule that a ceiling height is needed when quick-response sprinklers are stated is
 * enforced by calc-core's {@code RemoteAreaPlanInput}, not duplicated here.
 *
 * @param designAreaSqFt               the area of sprinkler operation before any reduction
 * @param densityGpmPerSqFt            design density
 * @param kFactor                      K of the sprinklers in the remote area
 * @param sprinklerSpacingFt           distance between sprinklers along a branch line
 * @param branchSpacingFt              distance between branch lines
 * @param quickResponseSprinklers      whether quick-response sprinklers are installed
 * @param ceilingHeightFt              ceiling height; required when quick-response is true
 * @param maxCoveragePerSprinklerSqFt  largest area one sprinkler may protect for this hazard; if
 *                                     given, spacing is checked against it
 */
public record RemoteAreaPlanRequest(
		@NotNull @Positive Double designAreaSqFt,
		@NotNull @Positive Double densityGpmPerSqFt,
		@NotNull @Positive Double kFactor,
		@NotNull @Positive Double sprinklerSpacingFt,
		@NotNull @Positive Double branchSpacingFt,
		@NotNull Boolean quickResponseSprinklers,
		@Positive Double ceilingHeightFt,
		@Positive Double maxCoveragePerSprinklerSqFt) {

}
