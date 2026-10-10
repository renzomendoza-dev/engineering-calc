package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.sprinkler.CFactorRow;

/**
 * One published Hazen-Williams C factor. {@code systemType} is {@code ANY} where the value does
 * not vary with wet/dry/preaction service.
 */
public record SprinklerCFactorEntryDto(
		String pipeType,
		String systemType,
		Double cFactor,
		String label,
		String confidence,
		String sourceNote) {

	public static SprinklerCFactorEntryDto from(CFactorRow row) {
		return new SprinklerCFactorEntryDto(
				row.pipeType(),
				row.systemType(),
				row.cFactor(),
				row.label(),
				row.confidence(),
				row.sourceNote());
	}

}
