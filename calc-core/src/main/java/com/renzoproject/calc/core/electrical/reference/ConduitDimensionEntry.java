package com.renzoproject.calc.core.electrical.reference;

/**
 * One row of PEC Table 10.1.1.4 — Dimensions and Percent Area of Conduit and Tubing.
 */
public record ConduitDimensionEntry(
		ConduitType conduitType,
		int racewaySizeMm,
		double over2Wires40PercentMm2,
		double sixtyPercentMm2,
		double oneWire53PercentMm2,
		double twoWires53PercentMm2,
		double nominalInternalDiameterMm,
		double totalArea100PercentMm2) {

}
