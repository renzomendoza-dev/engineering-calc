package com.renzoproject.calc.core.electrical.generatorset;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.electrical.generatorset.FirePumpSupply.FirePumpPlan;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorLoadSchedule.MotorRows;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetReference.StandbyClassEntry;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.OutputConductor;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.FirePumpResult;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.LoadRow;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.Requirement;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.StartingStep;
import com.renzoproject.calc.core.electrical.generatorset.MotorStartingCheck.StartingStepDraft;
import com.renzoproject.calc.core.electrical.motorflc.LockedRotorCalculator;
import com.renzoproject.calc.core.electrical.motorflc.MotorFlcCalculator;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingCalculator;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingInput;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingResult;
import com.renzoproject.calc.core.exception.CalculationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.renzoproject.calc.core.electrical.generatorset.GeneratorSetText.num;

/**
 * Sizes a standby generator set from the loads it will carry, following PEC 2017: the load is
 * built the way Article 2.20 builds a feeder load (lighting by occupancy with Table 2.20.3.3,
 * receptacles with Table 2.20.3.5, motors from the Article 4.30 full-load current tables), the
 * capacity rule comes from the Chapter 7 article for the standby class, and the output conductors
 * are sized from the set's nameplate current under 4.45.1.13.
 *
 * <p>The PEC does not say how to choose generator kVA from a load. The required kVA here is the
 * larger of the demand load plus an optional spare percentage and, when the generator's
 * subtransient reactance is given, what motor starting needs to stay within the voltage dip limit.
 * The standard sizes are typical catalogue ratings (see {@code standard-generator-sizes.json}) —
 * neither is a PEC figure, and the result says so.
 *
 * <p>This class puts the result together; the parts are worked out by {@link GeneratorLoadSchedule}
 * (the Article 2.20 load rows), {@link FirePumpSupply} (Article 6.95) and {@link MotorStartingCheck}
 * (starting steps and voltage dip).
 */
public class GeneratorSetSizingCalculator implements Calculator<GeneratorSetSizingInput, GeneratorSetSizingResult> {

	private static final double SQRT_3 = Math.sqrt(3.0);

	private final GeneratorSetReference reference = new GeneratorSetReference();
	private final WireSizingCalculator wireSizingCalculator = new WireSizingCalculator();
	private final GeneratorLoadSchedule loadSchedule;
	private final FirePumpSupply firePumpSupply;
	private final MotorStartingCheck motorStartingCheck;

	public GeneratorSetSizingCalculator() {
		MotorFlcCalculator motorFlcCalculator = new MotorFlcCalculator();
		LockedRotorCalculator lockedRotorCalculator = new LockedRotorCalculator();
		loadSchedule = new GeneratorLoadSchedule(reference, motorFlcCalculator);
		firePumpSupply = new FirePumpSupply(reference, motorFlcCalculator, lockedRotorCalculator, wireSizingCalculator);
		motorStartingCheck = new MotorStartingCheck(lockedRotorCalculator);
	}

	@Override
	public GeneratorSetSizingResult calculate(GeneratorSetSizingInput input) {
		double phaseFactor = input.phaseType() == MotorPhaseType.THREE_PHASE ? SQRT_3 : 1.0;
		List<String> warnings = new ArrayList<>();

		List<LoadRow> rows = new ArrayList<>();
		rows.addAll(loadSchedule.lightingRows(input.lightingAreas()));
		rows.addAll(loadSchedule.receptacleRows(input.receptacleGroups(), input.applyReceptacleDemandFactor()));
		rows.addAll(loadSchedule.otherRows(input.otherLoads()));
		MotorRows motorRows = loadSchedule.motorRows(input, phaseFactor);
		rows.addAll(motorRows.rows());
		rows.addAll(loadSchedule.directRows(input.directLoads(), phaseFactor, input.voltage()));
		FirePumpPlan firePumpPlan = input.firePump() == null ? null : firePumpSupply.firePumpPlan(input, phaseFactor, warnings);
		if (firePumpPlan != null) {
			rows.addAll(firePumpPlan.rows());
		}
		boolean hasStartingLoads = !input.motors().isEmpty() || firePumpPlan != null;

		double connectedVa = rows.stream().mapToDouble(LoadRow::connectedVoltAmperes).sum();
		double demandVa = rows.stream().mapToDouble(LoadRow::demandVoltAmperes).sum();
		double connectedKva = connectedVa / 1000.0;
		double demandKva = demandVa / 1000.0;
		double demandCurrentAmps = demandVa / (phaseFactor * input.voltage());
		double runningRequiredKva = demandKva * (1.0 + input.spareCapacityPercent() / 100.0);

		// What is already running before any motor or the fire pump starts; those are added step by
		// step. The pressure maintenance pump and the accessories are running before the fire pump starts.
		double alwaysRunningDemandVa = rows.stream()
				.filter(row -> row.kind() != LoadKind.MOTOR && row.kind() != LoadKind.FIRE_PUMP)
				.mapToDouble(LoadRow::demandVoltAmperes)
				.sum() + (firePumpPlan == null ? 0 : firePumpPlan.alwaysRunningVa());
		List<StartingStepDraft> startingDrafts = input.subtransientReactancePercent() == null || !hasStartingLoads
				? List.of()
				: motorStartingCheck.startingSteps(input, phaseFactor, motorRows, firePumpPlan, alwaysRunningDemandVa, warnings);
		Double startingRequiredKva = startingDrafts.isEmpty()
				? null
				: startingDrafts.stream().mapToDouble(StartingStepDraft::requiredGeneratorKva).max().getAsDouble();
		boolean startingGoverns = startingRequiredKva != null && startingRequiredKva > runningRequiredKva;
		double requiredKva = startingGoverns ? startingRequiredKva : runningRequiredKva;
		double requiredKw = requiredKva * input.generatorPowerFactor();

		Double loadFeederAmps = null;
		String loadFeederBasis = null;
		if (!input.motors().isEmpty()) {
			double largestMotorVa = motorRows.largestMotorVa();
			double otherMotorsVa = motorRows.totalMotorVa() - largestMotorVa;
			double nonMotorContinuousVa = 0;
			double nonMotorNoncontinuousVa = 0;
			for (LoadRow row : rows) {
				// A fire pump's supply conductors have their own rule (6.95.1.6(B)); it stays out of this one.
				if (row.kind() == LoadKind.MOTOR || row.kind() == LoadKind.FIRE_PUMP) {
					continue;
				}
				if (row.continuous()) {
					nonMotorContinuousVa += row.demandVoltAmperes();
				} else {
					nonMotorNoncontinuousVa += row.demandVoltAmperes();
				}
			}
			double feederVa = reference.largestMotorFactor() * largestMotorVa
					+ otherMotorsVa
					+ nonMotorNoncontinuousVa
					+ reference.continuousLoadFactor() * nonMotorContinuousVa;
			loadFeederAmps = feederVa / (phaseFactor * input.voltage());
			loadFeederBasis = String.format(Locale.ROOT,
					"4.30.2.4: 125%% of the largest motor (%s kVA) + the other motors (%s kVA) + 100%% of the noncontinuous load (%s kVA) + 125%% of the continuous load (%s kVA) = %s kVA, %s A at %d V.",
					num(largestMotorVa / 1000.0), num(otherMotorsVa / 1000.0), num(nonMotorNoncontinuousVa / 1000.0),
					num(nonMotorContinuousVa / 1000.0), num(feederVa / 1000.0), num(loadFeederAmps), input.voltage());
		}

		List<Double> standardSizes = reference.standardSizesKva();
		Double recommendedStandardKva = standardSizes.stream()
				.filter(size -> size >= requiredKva)
				.findFirst()
				.orElse(null);
		if (recommendedStandardKva == null && input.selectedGeneratorKva() == null) {
			warnings.add(String.format(Locale.ROOT,
					"The required %s kVA is above the largest standard size listed (%s kVA); split the load across more than one set or enter the set you intend to use.",
					num(requiredKva), num(standardSizes.get(standardSizes.size() - 1))));
		}

		boolean selectedIsUserEntered = input.selectedGeneratorKva() != null;
		Double selectedKva = selectedIsUserEntered ? input.selectedGeneratorKva() : recommendedStandardKva;
		Boolean selectedMeetsRequired = null;
		Double loadPercentOfSelected = null;
		Double nameplateCurrentAmps = null;
		Double minimumOutputAmpacity = null;
		WireSizingResult outputConductorResult = null;
		double outputFactor = input.designPreventsOverload()
				? reference.generatorOutputFactorWhereOverloadPrevented()
				: reference.generatorOutputFactor();

		if (selectedKva != null) {
			selectedMeetsRequired = selectedKva >= requiredKva;
			loadPercentOfSelected = demandKva / selectedKva * 100.0;
			nameplateCurrentAmps = selectedKva * 1000.0 / (phaseFactor * input.voltage());
			minimumOutputAmpacity = nameplateCurrentAmps * outputFactor;
			if (!selectedMeetsRequired) {
				warnings.add(String.format(Locale.ROOT,
						"The %s kVA set entered is smaller than the %s kVA required.", num(selectedKva), num(requiredKva)));
			}
			if (input.outputConductor() != null) {
				outputConductorResult = sizeOutputConductor(input.outputConductor(), minimumOutputAmpacity, warnings);
			}
		}

		if (hasStartingLoads && startingDrafts.isEmpty()) {
			warnings.add("Motor starting is not checked: starting current and the voltage dip it causes can call for a larger set than the running load does. Enter the generator's subtransient reactance and a maximum voltage dip to check it.");
		}
		final Double selectedForDip = selectedKva;
		List<StartingStep> startingStepResults = startingDrafts.stream()
				.map(draft -> {
					Double dip = selectedForDip == null
							? null
							: MotorStartingCheck.voltageDipPercent(input.subtransientReactancePercent(), draft.startingKva(), selectedForDip);
					Boolean withinLimit = dip == null ? null : dip <= draft.dipLimitPercent();
					return new StartingStep(draft.step(), draft.motors(), draft.startingKva(), draft.runningKvaBefore(),
							draft.requiredGeneratorKva(), dip, withinLimit, draft.dipLimitPercent());
				})
				.toList();
		String startingBasis = startingDrafts.isEmpty()
				? null
				: reference.lockedRotorBasis() + " " + reference.voltageDipBasis();

		FirePumpResult firePumpResult = firePumpPlan == null
				? null
				: firePumpSupply.firePumpResult(firePumpPlan, startingDrafts, startingStepResults, nameplateCurrentAmps);
		if (firePumpResult != null && firePumpResult.startingDropWithinLimit() != null && !firePumpResult.startingDropWithinLimit()) {
			warnings.add(String.format(Locale.ROOT,
					"At starting the voltage at the fire pump controller drops %s%%, more than the %s%% allowed (6.95.1.7).",
					num(firePumpResult.controllerStartingDropPercent()), num(reference.firePumpMaxStartingDropPercent())));
		}
		if (firePumpResult != null && firePumpResult.runningDropWithinLimit() != null && !firePumpResult.runningDropWithinLimit()) {
			warnings.add(String.format(Locale.ROOT,
					"Running at 115%% of full load the voltage at the fire pump controller drops %s%%, more than the %s%% allowed (6.95.1.7).",
					num(firePumpResult.cableRunningDropPercent()), num(reference.firePumpMaxRunningDropPercent())));
		}

		StandbyClassEntry standbyClass = reference.standbyClass(input.standbyClass());
		String capacityBasis = standbyClass.capacityBasis();
		String capacityCitation = standbyClass.capacityCitation();
		if (input.optionalTransfer() != null) {
			var transfer = reference.optionalTransfer(input.optionalTransfer());
			capacityBasis = capacityBasis + " " + transfer.text();
			capacityCitation = transfer.citation();
		}
		List<Requirement> requirements = standbyClass.requirements().stream()
				.map(entry -> new Requirement(entry.citation(), entry.text()))
				.toList();

		return new GeneratorSetSizingResult(
				rows,
				connectedKva,
				demandKva,
				demandCurrentAmps,
				input.spareCapacityPercent(),
				requiredKva,
				requiredKw,
				input.generatorPowerFactor(),
				loadFeederAmps,
				loadFeederBasis,
				recommendedStandardKva,
				selectedKva,
				selectedIsUserEntered,
				selectedMeetsRequired,
				loadPercentOfSelected,
				nameplateCurrentAmps,
				outputFactor,
				minimumOutputAmpacity,
				outputConductorResult,
				capacityCitation,
				capacityBasis,
				requirements,
				reference.standardSizesNote(),
				warnings,
				runningRequiredKva,
				startingRequiredKva,
				startingGoverns ? "MOTOR_STARTING" : "RUNNING_LOAD",
				startingStepResults,
				startingBasis,
				firePumpResult);
	}

	private WireSizingResult sizeOutputConductor(OutputConductor conditions, double minimumAmpacity, List<String> warnings) {
		// Passed as a non-continuous load so WireSizingCalculator applies no factor of its own: the
		// 115% (or 100%) of 4.45.1.13 is already in minimumAmpacity.
		WireSizingInput wireInput = new WireSizingInput(
				minimumAmpacity,
				false,
				conditions.ambientTempCelsius(),
				conditions.numberOfCurrentCarryingConductors(),
				conditions.numberOfParallelSets(),
				conditions.insulationType(),
				conditions.conductorMaterial(),
				conditions.terminationTempRatingCelsius(),
				null);
		try {
			return wireSizingCalculator.calculate(wireInput);
		} catch (CalculationException e) {
			warnings.add("Output conductor not sized: " + e.getMessage() + ".");
			return null;
		}
	}

}
