package com.renzoproject.calc.core.electrical.reference;

/**
 * One row of the insulation-type-to-temperature-rating mapping — which {@link AmpacityTable}
 * temperature column (60/75/90C) a given insulation type + conductor material combination
 * uses.
 */
public record InsulationTypeTempRatingEntry(
		InsulationType insulationType,
		ConductorMaterial conductorMaterial,
		int tempRatingCelsius) {

}
