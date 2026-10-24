package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Conditions for sizing the generator output conductors, mirroring calc-core's
 * {@code GeneratorSetSizingInput.OutputConductor}. The fields mean what they do in wire sizing.
 */
public record OutputConductorDto(
		double ambientTempCelsius,
		@Min(1) int numberOfCurrentCarryingConductors,
		@Min(1) int numberOfParallelSets,
		@NotBlank String insulationType,
		@NotBlank String conductorMaterial,
		@Min(60) int terminationTempRatingCelsius) {

}
