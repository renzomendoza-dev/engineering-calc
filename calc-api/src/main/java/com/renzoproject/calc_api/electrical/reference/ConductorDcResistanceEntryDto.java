package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.ConductorDcResistanceEntry;

/**
 * HTTP response representation of one row of PEC Table 10.1.1.8 (DC resistance), for
 * populating a frontend reference table. Values are as published (ohms per 305 meters) —
 * this is a display-only mirror of {@link ConductorDcResistanceEntry}, not used in any
 * calculation path.
 */
public record ConductorDcResistanceEntryDto(
		String sizeLabel,
		boolean stranded,
		int strandCount,
		double copperUncoatedOhmsPer305m,
		double copperCoatedOhmsPer305m,
		double aluminumOhmsPer305m) {

	public static ConductorDcResistanceEntryDto from(ConductorDcResistanceEntry entry) {
		return new ConductorDcResistanceEntryDto(
				entry.sizeLabel(),
				entry.stranded(),
				entry.strandCount(),
				entry.copperUncoatedOhmsPer305m(),
				entry.copperCoatedOhmsPer305m(),
				entry.aluminumOhmsPer305m());
	}

}
