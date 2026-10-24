package com.renzoproject.calc.core.electrical.generatorset;

import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * Input parameters for {@link GeneratorSetSizingCalculator}.
 *
 * @param standbyClass               which Chapter 7 article the source falls under
 * @param optionalTransfer           required and non-null when {@code standbyClass} is
 *                                    {@link StandbyClass#OPTIONAL}; must be {@code null} otherwise
 * @param phaseType                  {@link MotorPhaseType#SINGLE_PHASE} or
 *                                    {@link MotorPhaseType#THREE_PHASE} — the generator's output
 *                                    and the motors it feeds
 * @param voltage                    line-to-line system voltage; also the default voltage of every
 *                                    motor, which can differ per motor
 * @param lightingAreas              general lighting by floor area (may be empty)
 * @param receptacleGroups           receptacle outlets (may be empty)
 * @param applyReceptacleDemandFactor whether Table 2.20.3.5 reduces the receptacle load beyond 10 kVA
 * @param otherLoads                 any other fixed load with its own demand factor (may be empty)
 * @param motors                     motors, from the Article 4.30 full-load current tables (may be empty)
 * @param generatorPowerFactor       rating power factor of the generator set, in (0, 1]; sets the kW
 *                                    equivalent of the kVA. Not a PEC value.
 * @param spareCapacityPercent       extra capacity added over the demand load, in percent; not a PEC
 *                                    value, 0 for none
 * @param selectedGeneratorKva       the set to check instead of the suggested standard size;
 *                                    {@code null} to use the next standard size up
 * @param designPreventsOverload     whether the generator's design does not permit overloading,
 *                                    which lets the output conductors be sized at 100% instead of 115%
 * @param outputConductor            conditions for sizing the output conductors; {@code null} to
 *                                    report the minimum ampacity only
 * @throws CalculationException if any validation rule above is violated, or there is no load at all
 */
public record GeneratorSetSizingInput(
		StandbyClass standbyClass,
		OptionalStandbyTransfer optionalTransfer,
		MotorPhaseType phaseType,
		int voltage,
		List<LightingArea> lightingAreas,
		List<ReceptacleGroup> receptacleGroups,
		boolean applyReceptacleDemandFactor,
		List<OtherLoad> otherLoads,
		List<MotorLoad> motors,
		double generatorPowerFactor,
		double spareCapacityPercent,
		Double selectedGeneratorKva,
		boolean designPreventsOverload,
		OutputConductor outputConductor) {

	public GeneratorSetSizingInput {
		if (standbyClass == null) {
			throw new CalculationException("standbyClass is required");
		}
		if (standbyClass == StandbyClass.OPTIONAL) {
			if (optionalTransfer == null) {
				throw new CalculationException("optionalTransfer is required when standbyClass is OPTIONAL");
			}
		} else if (optionalTransfer != null) {
			throw new CalculationException("optionalTransfer must be null when standbyClass is not OPTIONAL");
		}
		if (phaseType != MotorPhaseType.SINGLE_PHASE && phaseType != MotorPhaseType.THREE_PHASE) {
			throw new CalculationException("phaseType must be SINGLE_PHASE or THREE_PHASE");
		}
		if (voltage <= 0) {
			throw new CalculationException("voltage must be positive");
		}
		lightingAreas = lightingAreas == null ? List.of() : List.copyOf(lightingAreas);
		receptacleGroups = receptacleGroups == null ? List.of() : List.copyOf(receptacleGroups);
		otherLoads = otherLoads == null ? List.of() : List.copyOf(otherLoads);
		motors = motors == null ? List.of() : List.copyOf(motors);
		if (lightingAreas.isEmpty() && receptacleGroups.isEmpty() && otherLoads.isEmpty() && motors.isEmpty()) {
			throw new CalculationException("Enter at least one load: lighting, receptacles, another load or a motor");
		}
		if (generatorPowerFactor <= 0 || generatorPowerFactor > 1) {
			throw new CalculationException("generatorPowerFactor must be greater than 0 and at most 1");
		}
		if (spareCapacityPercent < 0) {
			throw new CalculationException("spareCapacityPercent must not be negative");
		}
		if (selectedGeneratorKva != null && selectedGeneratorKva <= 0) {
			throw new CalculationException("selectedGeneratorKva must be positive");
		}
	}

	/**
	 * @param label                 name shown on the load schedule
	 * @param occupancy             key of a Table 2.20.2.3 occupancy
	 * @param areaType              key of the area type; {@code GENERAL} for the occupancy's own unit load
	 * @param floorAreaM2           floor area, calculated from the outside dimensions (2.20.2.3); positive
	 * @param unitLoadVoltAmperesPerM2 a design unit load above the table's; {@code null} to use the table.
	 *                              Below the table's minimum is rejected (2.20.2.3).
	 * @param entireLightingUsedAtOnce whether all of this lighting is likely to be used at one time (an
	 *                              operating room, ballroom, dining room); skips the demand factor for
	 *                              hospitals and hotels (2.20.3.3 note) and is ignored elsewhere
	 * @param continuous            whether the load runs three hours or more, for the 4.30.2.4 feeder check
	 */
	public record LightingArea(
			String label,
			String occupancy,
			String areaType,
			double floorAreaM2,
			Double unitLoadVoltAmperesPerM2,
			boolean entireLightingUsedAtOnce,
			boolean continuous) {

		public LightingArea {
			if (occupancy == null || occupancy.isBlank()) {
				throw new CalculationException("lighting area occupancy is required");
			}
			if (floorAreaM2 <= 0) {
				throw new CalculationException("lighting area floorAreaM2 must be positive");
			}
			if (unitLoadVoltAmperesPerM2 != null && unitLoadVoltAmperesPerM2 <= 0) {
				throw new CalculationException("lighting area unitLoadVoltAmperesPerM2 must be positive");
			}
			areaType = areaType == null || areaType.isBlank() ? "GENERAL" : areaType;
		}
	}

	/**
	 * @param label                  name shown on the load schedule
	 * @param outlets                number of receptacle outlets, each a single or multiple receptacle on one yoke
	 * @param bankOrOfficeFloorAreaM2 floor area when these outlets are in a bank or office building, where the
	 *                               load is the larger of 180 VA per outlet or 11 VA/m2 (2.20.2.5(K));
	 *                               {@code null} otherwise
	 * @param continuous             whether the load runs three hours or more, for the 4.30.2.4 feeder check
	 */
	public record ReceptacleGroup(String label, int outlets, Double bankOrOfficeFloorAreaM2, boolean continuous) {

		public ReceptacleGroup {
			if (outlets < 1) {
				throw new CalculationException("receptacle outlets must be at least 1");
			}
			if (bankOrOfficeFloorAreaM2 != null && bankOrOfficeFloorAreaM2 <= 0) {
				throw new CalculationException("receptacle bankOrOfficeFloorAreaM2 must be positive");
			}
		}
	}

	/**
	 * @param label                name shown on the load schedule
	 * @param voltAmperes          connected load in volt-amperes; positive
	 * @param demandFactorPercent  demand factor you apply, in (0, 100]
	 * @param continuous           whether the load runs three hours or more
	 */
	public record OtherLoad(String label, double voltAmperes, double demandFactorPercent, boolean continuous) {

		public OtherLoad {
			if (voltAmperes <= 0) {
				throw new CalculationException("other load voltAmperes must be positive");
			}
			if (demandFactorPercent <= 0 || demandFactorPercent > 100) {
				throw new CalculationException("other load demandFactorPercent must be greater than 0 and at most 100");
			}
		}
	}

	/**
	 * @param label           name shown on the load schedule
	 * @param horsepowerLabel horsepower label as in the PEC table, e.g. {@code "7 1/2"}
	 * @param voltage         motor voltage; must have a published full-load current at that horsepower
	 * @param quantity        number of identical motors running together; at least 1
	 */
	public record MotorLoad(String label, String horsepowerLabel, int voltage, int quantity) {

		public MotorLoad {
			if (horsepowerLabel == null || horsepowerLabel.isBlank()) {
				throw new CalculationException("motor horsepowerLabel is required");
			}
			if (voltage <= 0) {
				throw new CalculationException("motor voltage must be positive");
			}
			if (quantity < 1) {
				throw new CalculationException("motor quantity must be at least 1");
			}
		}
	}

	/**
	 * Conditions for sizing the conductors from the generator terminals; see
	 * {@link com.renzoproject.calc.core.electrical.wiresizing.WireSizingInput} for what each means.
	 */
	public record OutputConductor(
			double ambientTempCelsius,
			int numberOfCurrentCarryingConductors,
			int numberOfParallelSets,
			InsulationType insulationType,
			ConductorMaterial conductorMaterial,
			int terminationTempRatingCelsius) {
	}

}
