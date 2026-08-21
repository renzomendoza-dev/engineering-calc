package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.AmbientTempCorrectionEntry;

/**
 * HTTP response representation of one row of PEC Table 3.10.2.6(B)(2)(a) (ambient temperature
 * correction factors, 30C base), for populating a frontend reference table — display only,
 * not used in any calculation path.
 */
public record AmbientTempCorrectionEntryDto(
		String ambientTempRangeLabel,
		Double ambientTempLowC,
		double ambientTempHighC,
		Double factor60C,
		Double factor75C,
		Double factor90C) {

	public static AmbientTempCorrectionEntryDto from(AmbientTempCorrectionEntry entry) {
		return new AmbientTempCorrectionEntryDto(
				entry.ambientTempRangeLabel(),
				entry.ambientTempLowC(),
				entry.ambientTempHighC(),
				entry.factor60C(),
				entry.factor75C(),
				entry.factor90C());
	}

}
