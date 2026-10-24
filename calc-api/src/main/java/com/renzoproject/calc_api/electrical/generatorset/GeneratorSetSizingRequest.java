package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * HTTP request body for generator set sizing.
 *
 * <p>{@code standbyClass}, {@code optionalTransfer} and {@code phaseType} are plain strings
 * mapped explicitly in {@link GeneratorSetSizingMapper} so a bad value surfaces as a specific 400
 * (see {@code EnumParsing}). The cross-field rule — a transfer mode only for optional standby — is
 * left to calc-core's {@code GeneratorSetSizingInput}.
 *
 * @param standbyClass                EMERGENCY, LEGALLY_REQUIRED or OPTIONAL
 * @param optionalTransfer            MANUAL, AUTOMATIC_FULL_LOAD or AUTOMATIC_LOAD_MANAGEMENT; only with OPTIONAL
 * @param phaseType                   SINGLE_PHASE or THREE_PHASE
 * @param voltage                     line-to-line system voltage
 * @param lightingAreas               general lighting by area
 * @param receptacleGroups            receptacle outlets
 * @param applyReceptacleDemandFactor whether Table 2.20.3.5 reduces the receptacle load beyond 10 kVA
 * @param otherLoads                  any other fixed load
 * @param motors                      motors, from the Article 4.30 full-load current tables
 * @param generatorPowerFactor        rating power factor of the set, above 0 and at most 1
 * @param spareCapacityPercent        extra capacity over the demand load, in percent
 * @param selectedGeneratorKva        a set to check instead of the suggested standard size; blank to use the suggestion
 * @param designPreventsOverload      whether the generator's design prevents overloading (100% instead of 115%)
 * @param outputConductor             conditions for sizing the output conductors; blank for the ampacity only
 * @param subtransientReactancePercent the generator's subtransient reactance X"d, in percent; blank skips the motor
 *                                    starting check. Must be given with {@code maximumVoltageDipPercent}.
 * @param maximumVoltageDipPercent    the largest voltage dip accepted when a motor starts, in percent; blank skips
 *                                    the motor starting check
 * @param directLoads                 a building's total load or a fire pump entered as a figure, in place of
 *                                    (or as well as) the lists above
 */
public record GeneratorSetSizingRequest(
		@NotBlank String standbyClass,
		String optionalTransfer,
		@NotBlank String phaseType,
		@Positive int voltage,
		@Size(max = 200) List<@Valid LightingAreaDto> lightingAreas,
		@Size(max = 200) List<@Valid ReceptacleGroupDto> receptacleGroups,
		boolean applyReceptacleDemandFactor,
		@Size(max = 200) List<@Valid OtherLoadDto> otherLoads,
		@Size(max = 200) List<@Valid MotorLoadDto> motors,
		@Positive double generatorPowerFactor,
		@PositiveOrZero double spareCapacityPercent,
		@Positive Double selectedGeneratorKva,
		boolean designPreventsOverload,
		@Valid OutputConductorDto outputConductor,
		@Positive Double subtransientReactancePercent,
		@Positive Double maximumVoltageDipPercent,
		@Size(max = 200) List<@Valid DirectLoadDto> directLoads) {

}
