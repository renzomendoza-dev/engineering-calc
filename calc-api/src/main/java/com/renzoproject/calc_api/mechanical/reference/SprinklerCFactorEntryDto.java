package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.sprinkler.CFactorRow;

/**
 * One published Hazen-Williams C factor. {@code systemType} is {@code ANY} where the value does
 * not vary with wet/dry/preaction service. The epsilon (absolute roughness) figures NFPA 13's
 * annex pairs with each C value are passed through for display; the calculation itself uses C.
 */
public record SprinklerCFactorEntryDto(
		String pipeType,
		String systemType,
		Double cFactor,
		Double epsilonFactorInch,
		Double epsilonFactorMm,
		String label,
		String confidence,
		String sourceNote) {

	public static SprinklerCFactorEntryDto from(CFactorRow row) {
		return new SprinklerCFactorEntryDto(
				row.pipeType(),
				row.systemType(),
				row.cFactor(),
				row.epsilonFactorInch(),
				row.epsilonFactorMm(),
				row.label(),
				row.confidence(),
				row.sourceNote());
	}

}
