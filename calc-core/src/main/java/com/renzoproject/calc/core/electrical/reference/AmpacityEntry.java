package com.renzoproject.calc.core.electrical.reference;

/**
 * One row of PEC Table 3.10.2.6(B)(16) — allowable ampacities for insulated conductors up to
 * 2000V, not more than three current-carrying conductors in raceway/cable/directly buried,
 * based on 30C ambient.
 */
public record AmpacityEntry(
		ConductorMaterial conductorMaterial,
		String sizeLabel,
		int tempRatingCelsius,
		double ampacityAmps) {

}
