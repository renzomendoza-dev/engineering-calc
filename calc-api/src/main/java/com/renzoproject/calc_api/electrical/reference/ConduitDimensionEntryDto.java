package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.ConduitDimensionEntry;

/**
 * HTTP response representation of one row of PEC Table 10.1.1.4 (dimensions and percent
 * area of conduit and tubing), for populating a frontend reference table. conduitType is
 * serialized as its published label (e.g. {@code "EMT"}), not the Java enum constant name,
 * matching the convention used elsewhere in this package (see
 * {@code ConductorReferenceService.listConduitFillTypes}). Display-only — not used in any
 * calculation path.
 */
public record ConduitDimensionEntryDto(
		String conduitType,
		int racewaySizeMm,
		double over2Wires40PercentMm2,
		double sixtyPercentMm2,
		double oneWire53PercentMm2,
		double twoWires53PercentMm2,
		double nominalInternalDiameterMm,
		double totalArea100PercentMm2) {

	public static ConduitDimensionEntryDto from(ConduitDimensionEntry entry) {
		return new ConduitDimensionEntryDto(
				entry.conduitType().toLabel(),
				entry.racewaySizeMm(),
				entry.over2Wires40PercentMm2(),
				entry.sixtyPercentMm2(),
				entry.oneWire53PercentMm2(),
				entry.twoWires53PercentMm2(),
				entry.nominalInternalDiameterMm(),
				entry.totalArea100PercentMm2());
	}

}
