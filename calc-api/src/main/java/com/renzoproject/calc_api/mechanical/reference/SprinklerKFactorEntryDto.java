package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.sprinkler.SprinklerKFactorRow;

/**
 * One published nominal sprinkler K-factor, for a form's dropdown. {@code threadType} is the
 * published thread size, not an orifice size. {@code kFactorRangeMinGpm}/{@code MaxGpm} bound the
 * actual K a listed sprinkler may carry under this nominal value.
 */
public record SprinklerKFactorEntryDto(
		Double nominalK,
		Double nominalKLpmBar,
		Double kFactorRangeMinGpm,
		Double kFactorRangeMaxGpm,
		Double percentOfK56Discharge,
		String threadType,
		String label,
		String confidence) {

	public static SprinklerKFactorEntryDto from(SprinklerKFactorRow row) {
		return new SprinklerKFactorEntryDto(
				row.nominalK(),
				row.nominalKLpmBar(),
				row.kFactorRangeMinGpm(),
				row.kFactorRangeMaxGpm(),
				row.percentOfK56Discharge(),
				row.threadType(),
				row.label(),
				row.confidence());
	}

}
