package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.ConductorCountAdjustmentEntry;

/**
 * HTTP response representation of one row of PEC Table 3.10.2.6(B)(3)(a) (ampacity adjustment
 * for more than three current-carrying conductors), for populating a frontend reference
 * table — display only, not used in any calculation path.
 */
public record ConductorCountAdjustmentEntryDto(
		String conductorCountRangeLabel,
		int conductorCountMin,
		Integer conductorCountMax,
		double adjustmentFactorPercent) {

	public static ConductorCountAdjustmentEntryDto from(ConductorCountAdjustmentEntry entry) {
		return new ConductorCountAdjustmentEntryDto(
				entry.conductorCountRangeLabel(),
				entry.conductorCountMin(),
				entry.conductorCountMax(),
				entry.adjustmentFactorPercent());
	}

}
