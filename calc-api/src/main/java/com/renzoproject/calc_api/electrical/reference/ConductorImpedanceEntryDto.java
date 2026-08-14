package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.ConductorImpedanceEntry;

/**
 * HTTP response representation of one row of PEC Table 10.1.1.9 (AC resistance and
 * reactance), for populating a frontend reference table. Values are as published (ohms
 * per 305 meters) — this is a display-only mirror of {@link ConductorImpedanceEntry}, not
 * used in any calculation path.
 */
public record ConductorImpedanceEntryDto(
		String sizeLabel,
		double reactancePvcAluminumConduitOhmsPer305m,
		double reactanceSteelConduitOhmsPer305m,
		Double resistanceCopperPvcOhmsPer305m,
		Double resistanceCopperAluminumConduitOhmsPer305m,
		Double resistanceCopperSteelOhmsPer305m,
		Double resistanceAluminumWirePvcOhmsPer305m,
		Double resistanceAluminumWireAluminumConduitOhmsPer305m,
		Double resistanceAluminumWireSteelOhmsPer305m) {

	public static ConductorImpedanceEntryDto from(ConductorImpedanceEntry entry) {
		return new ConductorImpedanceEntryDto(
				entry.sizeLabel(),
				entry.reactancePvcAluminumConduitOhmsPer305m(),
				entry.reactanceSteelConduitOhmsPer305m(),
				entry.resistanceCopperPvcOhmsPer305m(),
				entry.resistanceCopperAluminumConduitOhmsPer305m(),
				entry.resistanceCopperSteelOhmsPer305m(),
				entry.resistanceAluminumWirePvcOhmsPer305m(),
				entry.resistanceAluminumWireAluminumConduitOhmsPer305m(),
				entry.resistanceAluminumWireSteelOhmsPer305m());
	}

}
