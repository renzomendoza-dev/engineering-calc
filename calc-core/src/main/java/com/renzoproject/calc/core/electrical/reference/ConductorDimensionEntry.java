package com.renzoproject.calc.core.electrical.reference;

/**
 * One row of PEC Table 10.1.1.5 — Dimensions of Insulated Conductors and Fixture Wires.
 */
public record ConductorDimensionEntry(
		InsulationType insulationType,
		boolean withoutOuterCovering,
		String sizeLabel,
		double approximateAreaMm2,
		double approximateDiameterMm) {

}
