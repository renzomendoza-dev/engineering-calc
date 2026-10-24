package com.renzoproject.calc.core.electrical.generatorset;

import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
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
 * @param subtransientReactancePercent the generator's subtransient reactance X"d in percent, from its
 *                                    data sheet; with {@code maximumVoltageDipPercent}, switches on the
 *                                    motor starting check. {@code null} for neither. Not a PEC value.
 * @param maximumVoltageDipPercent   the largest voltage dip you accept when a motor starts, in percent
 *                                    (above 0, below 100); {@code null} for no starting check. Your own
 *                                    criterion, not a PEC value (6.95.1.17's 15% is for fire pumps).
 * @param directLoads                a building's total load entered as a figure, in place of building it
 *                                    up from lighting, receptacles and motors (may be empty)
 * @param firePump                   a fire pump the generator also carries, with its Article 6.95 checks;
 *                                    {@code null} for none
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
		OutputConductor outputConductor,
		Double subtransientReactancePercent,
		Double maximumVoltageDipPercent,
		List<DirectLoad> directLoads,
		FirePump firePump) {

	/** Without a fire pump. */
	public GeneratorSetSizingInput(
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
			OutputConductor outputConductor,
			Double subtransientReactancePercent,
			Double maximumVoltageDipPercent,
			List<DirectLoad> directLoads) {
		this(standbyClass, optionalTransfer, phaseType, voltage, lightingAreas, receptacleGroups,
				applyReceptacleDemandFactor, otherLoads, motors, generatorPowerFactor, spareCapacityPercent,
				selectedGeneratorKva, designPreventsOverload, outputConductor,
				subtransientReactancePercent, maximumVoltageDipPercent, directLoads, null);
	}

	/** Without loads entered as a total. */
	public GeneratorSetSizingInput(
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
			OutputConductor outputConductor,
			Double subtransientReactancePercent,
			Double maximumVoltageDipPercent) {
		this(standbyClass, optionalTransfer, phaseType, voltage, lightingAreas, receptacleGroups,
				applyReceptacleDemandFactor, otherLoads, motors, generatorPowerFactor, spareCapacityPercent,
				selectedGeneratorKva, designPreventsOverload, outputConductor,
				subtransientReactancePercent, maximumVoltageDipPercent, null, null);
	}

	/** Without the motor starting check: the running-load sizing of the first slice. */
	public GeneratorSetSizingInput(
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
		this(standbyClass, optionalTransfer, phaseType, voltage, lightingAreas, receptacleGroups,
				applyReceptacleDemandFactor, otherLoads, motors, generatorPowerFactor, spareCapacityPercent,
				selectedGeneratorKva, designPreventsOverload, outputConductor, null, null, null, null);
	}

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
		directLoads = directLoads == null ? List.of() : List.copyOf(directLoads);
		if (lightingAreas.isEmpty() && receptacleGroups.isEmpty() && otherLoads.isEmpty() && motors.isEmpty()
				&& directLoads.isEmpty() && firePump == null) {
			throw new CalculationException(
					"Enter at least one load: lighting, receptacles, another load, a motor, a total load or a fire pump");
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
		if ((subtransientReactancePercent == null) != (maximumVoltageDipPercent == null)) {
			throw new CalculationException(
					"Enter both the generator's subtransient reactance and the maximum voltage dip, or neither");
		}
		if (subtransientReactancePercent != null && (subtransientReactancePercent <= 0 || subtransientReactancePercent > 100)) {
			throw new CalculationException("subtransientReactancePercent must be greater than 0 and at most 100");
		}
		if (maximumVoltageDipPercent != null && (maximumVoltageDipPercent <= 0 || maximumVoltageDipPercent >= 100)) {
			throw new CalculationException("maximumVoltageDipPercent must be greater than 0 and less than 100");
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
	 * @param startStep       when it starts: motors with the same step start together, a higher step starts
	 *                        once the lower steps are running; at least 1
	 * @param startingCurrentPercent starting current as a percent of the table's locked-rotor current, in
	 *                        (0, 100]; 100 for across-the-line starting, less where a starter or drive holds
	 *                        the current down. Not a PEC value.
	 */
	public record MotorLoad(
			String label, String horsepowerLabel, int voltage, int quantity, int startStep, double startingCurrentPercent) {

		/** Across the line, in the first step. */
		public MotorLoad(String label, String horsepowerLabel, int voltage, int quantity) {
			this(label, horsepowerLabel, voltage, quantity, 1, 100.0);
		}

		public MotorLoad {
			if (startStep < 1) {
				throw new CalculationException("motor startStep must be at least 1");
			}
			if (startingCurrentPercent <= 0 || startingCurrentPercent > 100) {
				throw new CalculationException("motor startingCurrentPercent must be greater than 0 and at most 100");
			}
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
	 * A building's whole load entered as a total rather than built up. The figure is taken as the
	 * load to carry, at 100%: enter it after your own demand factors. (A fire pump is entered as a
	 * {@link FirePump}, which also carries its Article 6.95 checks.)
	 *
	 * @param label                  name shown on the load schedule
	 * @param amount                 the load, in {@code unit}; positive
	 * @param unit                   kVA, kW (needs {@code powerFactor}) or amperes at the system voltage
	 * @param powerFactor            power factor of the load, in (0, 1]; required for kW, ignored otherwise
	 * @param continuous             whether the load runs three hours or more
	 */
	public record DirectLoad(
			String label,
			double amount,
			DirectLoadUnit unit,
			Double powerFactor,
			boolean continuous) {

		public DirectLoad {
			if (unit == null) {
				throw new CalculationException("total load unit is required");
			}
			if (amount <= 0) {
				throw new CalculationException("total load amount must be positive");
			}
			if (unit == DirectLoadUnit.KW) {
				if (powerFactor == null || powerFactor <= 0 || powerFactor > 1) {
					throw new CalculationException("a load in kW needs a power factor greater than 0 and at most 1");
				}
			}
		}
	}

	/**
	 * A pump motor, entered either from the PEC horsepower tables or as the currents from its
	 * controller or nameplate. Exactly one of the two ways must be given.
	 *
	 * @param horsepowerLabel horsepower as printed in the PEC tables, with {@code voltage}
	 * @param voltage         motor voltage for the table lookup
	 * @param fullLoadAmps    full-load current in amperes at the system voltage
	 * @param lockedRotorAmps locked-rotor current in amperes at the system voltage; for the fire pump
	 *                        motor only, and optional (without it its starting is left out of the check)
	 */
	public record PumpMotor(String horsepowerLabel, Integer voltage, Double fullLoadAmps, Double lockedRotorAmps) {

		public PumpMotor {
			boolean byHorsepower = horsepowerLabel != null && !horsepowerLabel.isBlank();
			boolean byAmps = fullLoadAmps != null;
			if (byHorsepower == byAmps) {
				throw new CalculationException("Enter a pump either by horsepower and voltage or by its full-load current");
			}
			if (byHorsepower) {
				if (voltage == null || voltage <= 0) {
					throw new CalculationException("a pump entered by horsepower needs its voltage");
				}
				if (lockedRotorAmps != null) {
					throw new CalculationException("lockedRotorAmps is only for a pump entered by its currents; a horsepower comes from the PEC tables");
				}
			} else {
				if (fullLoadAmps <= 0) {
					throw new CalculationException("a pump's fullLoadAmps must be positive");
				}
				if (lockedRotorAmps != null && lockedRotorAmps <= 0) {
					throw new CalculationException("a pump's lockedRotorAmps must be positive");
				}
			}
		}

		boolean byHorsepower() {
			return horsepowerLabel != null && !horsepowerLabel.isBlank();
		}
	}

	/**
	 * The run of conductors from the generator to the fire pump controller, sized to Article 6.95
	 * and checked against its voltage drop limits.
	 *
	 * @param ambientTempCelsius                 ambient temperature the conductors run through
	 * @param numberOfCurrentCarryingConductors  current-carrying conductors in the same raceway; at least 1
	 * @param numberOfParallelSets               identical conductors in parallel; at least 1
	 * @param insulationType                     conductor insulation type
	 * @param conductorMaterial                  conductor material
	 * @param terminationTempRatingCelsius       temperature rating of the terminations: 60, 75 or 90
	 * @param oneWayLengthMeters                 length of the run from the generator to the controller;
	 *                                           {@code null} skips the voltage drop checks
	 * @param conduitMaterial                    conduit material, for the conductor reactance; required with
	 *                                           the length
	 * @param startingPowerFactor                power factor of the pump at locked rotor, in (0, 1]; required
	 *                                           with the length. Not a PEC value.
	 * @param runningPowerFactor                 power factor of the pump at 115% of full load, in (0, 1];
	 *                                           required with the length. Not a PEC value.
	 * @param cableStartingDropAllowancePercent  the part of the 15% starting drop given to the cable, in
	 *                                           (0, 15); the conductor is sized so it stays within it, and the
	 *                                           generator gets what the cable actually leaves. A design
	 *                                           choice, not a PEC value; required with the length.
	 */
	public record SupplyRun(
			double ambientTempCelsius,
			int numberOfCurrentCarryingConductors,
			int numberOfParallelSets,
			InsulationType insulationType,
			ConductorMaterial conductorMaterial,
			int terminationTempRatingCelsius,
			Double oneWayLengthMeters,
			ConduitMaterial conduitMaterial,
			Double startingPowerFactor,
			Double runningPowerFactor,
			Double cableStartingDropAllowancePercent) {

		public SupplyRun {
			if (numberOfCurrentCarryingConductors < 1) {
				throw new CalculationException("fire pump numberOfCurrentCarryingConductors must be at least 1");
			}
			if (numberOfParallelSets < 1) {
				throw new CalculationException("fire pump numberOfParallelSets must be at least 1");
			}
			if (terminationTempRatingCelsius != 60 && terminationTempRatingCelsius != 75 && terminationTempRatingCelsius != 90) {
				throw new CalculationException("fire pump terminationTempRatingCelsius must be 60, 75, or 90");
			}
			if (oneWayLengthMeters != null) {
				if (oneWayLengthMeters <= 0) {
					throw new CalculationException("the fire pump run's oneWayLengthMeters must be positive");
				}
				if (conduitMaterial == null || startingPowerFactor == null || runningPowerFactor == null
						|| cableStartingDropAllowancePercent == null) {
					throw new CalculationException(
							"A run length needs the conduit material, both power factors and the cable's share of the starting drop");
				}
				if (startingPowerFactor <= 0 || startingPowerFactor > 1 || runningPowerFactor <= 0 || runningPowerFactor > 1) {
					throw new CalculationException("the fire pump power factors must be greater than 0 and at most 1");
				}
				if (cableStartingDropAllowancePercent <= 0 || cableStartingDropAllowancePercent >= 15) {
					throw new CalculationException("the cable's share of the starting drop must be greater than 0 and less than 15 percent");
				}
			}
		}
	}

	/**
	 * A fire pump the generator carries as the alternate source (PEC Article 6.95): the pump, an
	 * optional pressure maintenance (jockey) pump and accessories, and the run to its controller.
	 *
	 * @param motor                   the fire pump motor
	 * @param jockey                  the pressure maintenance pump; {@code null} for none. Taken as running
	 *                                before the fire pump starts; only its full-load current is used.
	 * @param accessoryAmps           full-load current of the associated accessory equipment in amperes at
	 *                                the system voltage; {@code null} for none
	 * @param startStep               start step of the fire pump motor, as for other motors; at least 1
	 * @param startingCurrentPercent  starting current as a percent of the locked-rotor current, in (0, 100]
	 * @param supplyRun               conductors to the controller; {@code null} to skip their sizing
	 */
	public record FirePump(
			PumpMotor motor,
			PumpMotor jockey,
			Double accessoryAmps,
			int startStep,
			double startingCurrentPercent,
			SupplyRun supplyRun) {

		public FirePump {
			if (motor == null) {
				throw new CalculationException("the fire pump motor is required");
			}
			if (jockey != null && jockey.lockedRotorAmps() != null) {
				throw new CalculationException("the pressure maintenance pump takes no locked-rotor current");
			}
			if (accessoryAmps != null && accessoryAmps < 0) {
				throw new CalculationException("accessoryAmps must not be negative");
			}
			if (startStep < 1) {
				throw new CalculationException("fire pump startStep must be at least 1");
			}
			if (startingCurrentPercent <= 0 || startingCurrentPercent > 100) {
				throw new CalculationException("fire pump startingCurrentPercent must be greater than 0 and at most 100");
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
