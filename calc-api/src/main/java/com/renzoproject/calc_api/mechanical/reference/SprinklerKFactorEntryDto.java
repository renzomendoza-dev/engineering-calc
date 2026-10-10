package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.sprinkler.SprinklerKFactorRow;

/**
 * One published nominal sprinkler K-factor, for a form's dropdown. {@code orificeNominal} is
 * {@code null} where the orifice size isn't confirmed.
 */
public record SprinklerKFactorEntryDto(
		Double nominalK,
		String orificeNominal,
		String label,
		String confidence) {

	public static SprinklerKFactorEntryDto from(SprinklerKFactorRow row) {
		return new SprinklerKFactorEntryDto(
				row.nominalK(),
				row.orificeNominal(),
				row.label(),
				row.confidence());
	}

}
