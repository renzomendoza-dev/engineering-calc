package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.ConductorDimensionEntry;

/**
 * HTTP response representation of one row of PEC Table 10.1.1.5 (dimensions of insulated
 * conductors and fixture wires), for populating a frontend reference table. insulationType
 * is serialized as its published label (e.g. {@code "THHN"}), not the Java enum constant
 * name, matching the convention used elsewhere in this package (see
 * {@code ConductorReferenceService.listInsulationTypes}). Display-only — not used in any
 * calculation path.
 */
public record ConductorDimensionEntryDto(
		String insulationType,
		boolean withoutOuterCovering,
		String sizeLabel,
		double approximateAreaMm2,
		double approximateDiameterMm) {

	public static ConductorDimensionEntryDto from(ConductorDimensionEntry entry) {
		return new ConductorDimensionEntryDto(
				entry.insulationType().toLabel(),
				entry.withoutOuterCovering(),
				entry.sizeLabel(),
				entry.approximateAreaMm2(),
				entry.approximateDiameterMm());
	}

}
