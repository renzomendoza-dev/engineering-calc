package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * The run from the generator to the fire pump controller, mirroring calc-core's
 * {@code GeneratorSetSizingInput.SupplyRun}. The conductor fields mean what they do in wire sizing;
 * the length and what follows it switch on the voltage drop checks.
 *
 * @param oneWayLengthMeters                length of the run; blank skips the voltage drop checks
 * @param conduitMaterial                   PVC, STEEL or ALUMINUM; required with the length
 * @param startingPowerFactor               power factor of the pump at locked rotor; required with the length
 * @param runningPowerFactor                power factor of the pump at 115% of full load; required with the length
 * @param cableStartingDropAllowancePercent the part of the 15% starting drop given to the cable, below 15;
 *                                          required with the length
 */
public record SupplyRunDto(
		double ambientTempCelsius,
		@Min(1) int numberOfCurrentCarryingConductors,
		@Min(1) int numberOfParallelSets,
		@NotBlank String insulationType,
		@NotBlank String conductorMaterial,
		@Min(60) int terminationTempRatingCelsius,
		@Positive Double oneWayLengthMeters,
		String conduitMaterial,
		@Positive Double startingPowerFactor,
		@Positive Double runningPowerFactor,
		@Positive Double cableStartingDropAllowancePercent) {

}
