package com.renzoproject.calc_api.mechanical.sprinkler;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Fittings of one type on a pipe segment.
 *
 * <p>Either {@code fittingType} (looked up in the published equivalent-length table) or
 * {@code explicitEquivalentLengthFt} is required -- that either/or rule is enforced by calc-core's
 * {@code SegmentFitting}, not duplicated here.
 *
 * @param fittingType                 key from the fitting equivalent length reference table
 * @param quantity                    how many of this fitting on the segment
 * @param explicitEquivalentLengthFt  equivalent length per fitting in feet, bypassing the lookup
 */
public record SegmentFittingDto(
		String fittingType,
		@NotNull @Positive Integer quantity,
		@Positive Double explicitEquivalentLengthFt) {

}
