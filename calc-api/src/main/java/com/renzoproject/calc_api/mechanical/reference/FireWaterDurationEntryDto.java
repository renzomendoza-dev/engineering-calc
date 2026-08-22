package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.storage.FireWaterDurationEntry;

/**
 * HTTP response representation of one row of reference/storage/fire-water-duration.json, for
 * populating a frontend reference table — display only, not used in any calculation path.
 * hazardClassification is serialized as its enum name.
 */
public record FireWaterDurationEntryDto(
		String hazardClassification,
		double minMinutes,
		double maxMinutes,
		double hoseStreamAllowanceGpm) {

	public static FireWaterDurationEntryDto from(FireWaterDurationEntry entry) {
		return new FireWaterDurationEntryDto(
				entry.hazardClassification().name(),
				entry.minMinutes(),
				entry.maxMinutes(),
				entry.hoseStreamAllowanceGpm());
	}

}
