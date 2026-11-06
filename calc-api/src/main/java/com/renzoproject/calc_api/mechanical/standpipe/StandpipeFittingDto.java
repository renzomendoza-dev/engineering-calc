package com.renzoproject.calc_api.mechanical.standpipe;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Fittings of one type on a segment, mirroring calc-core StandpipeFitting. Either
 * {@code fittingType} (a key from the reference table) or {@code explicitEquivalentLengthFt} is
 * required; calc-core enforces that.
 */
public record StandpipeFittingDto(
		String fittingType,
		@NotNull @Positive Integer quantity,
		@Positive Double explicitEquivalentLengthFt) {

}
