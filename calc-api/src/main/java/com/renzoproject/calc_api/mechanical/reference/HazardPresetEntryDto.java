package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.sprinkler.HazardPresetRow;

/**
 * One published NFPA 13 design preset, for a sprinkler form's hazard dropdown. {@code confidence}
 * is exposed so a caller can badge values that are not yet verified against the printed standard.
 */
public record HazardPresetEntryDto(
		String hazard,
		String label,
		Double densityGpmPerSqFt,
		Double designAreaSqFt,
		Double hoseStreamGpm,
		Integer durationMinutes,
		Double maxCoverageSqFtPerSprinkler,
		String confidence,
		String sourceNote) {

	public static HazardPresetEntryDto from(HazardPresetRow row) {
		return new HazardPresetEntryDto(
				row.hazard(),
				row.label(),
				row.densityGpmPerSqFt(),
				row.designAreaSqFt(),
				row.hoseStreamGpm(),
				row.durationMinutes(),
				row.maxCoverageSqFtPerSprinkler(),
				row.confidence(),
				row.sourceNote());
	}

}
