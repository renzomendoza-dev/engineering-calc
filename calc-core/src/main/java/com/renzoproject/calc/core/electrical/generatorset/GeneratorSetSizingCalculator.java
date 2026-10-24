package com.renzoproject.calc.core.electrical.generatorset;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetReference.AreaType;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetReference.DemandGroup;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetReference.DemandTier;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetReference.Occupancy;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetReference.StandbyClassEntry;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.DirectLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.LightingArea;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.MotorLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.OtherLoad;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.OutputConductor;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.ReceptacleGroup;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.LoadRow;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.Requirement;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.StartingStep;
import com.renzoproject.calc.core.electrical.motorflc.LockedRotorCalculator;
import com.renzoproject.calc.core.electrical.motorflc.LockedRotorInput;
import com.renzoproject.calc.core.electrical.motorflc.MotorFlcCalculator;
import com.renzoproject.calc.core.electrical.motorflc.MotorFlcInput;
import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingCalculator;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingInput;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingResult;
import com.renzoproject.calc.core.exception.CalculationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Sizes a standby generator set from the loads it will carry, following PEC 2017: the load is
 * built the way Article 2.20 builds a feeder load (lighting by occupancy with Table 2.20.3.3,
 * receptacles with Table 2.20.3.5, motors from the Article 4.30 full-load current tables), the
 * capacity rule comes from the Chapter 7 article for the standby class, and the output conductors
 * are sized from the set's nameplate current under 4.45.1.13.
 *
 * <p>The PEC does not say how to choose generator kVA from a load. The required kVA here is the
 * demand load plus an optional spare percentage, and the standard sizes are typical catalogue
 * ratings (see {@code standard-generator-sizes.json}) — neither is a PEC figure, and the result
 * says so. Motor starting is not part of this calculator.
 */
public class GeneratorSetSizingCalculator implements Calculator<GeneratorSetSizingInput, GeneratorSetSizingResult> {

	private static final double SQRT_3 = Math.sqrt(3.0);

	private final GeneratorSetReference reference = new GeneratorSetReference();
	private final MotorFlcCalculator motorFlcCalculator = new MotorFlcCalculator();
	private final WireSizingCalculator wireSizingCalculator = new WireSizingCalculator();
	private final LockedRotorCalculator lockedRotorCalculator = new LockedRotorCalculator();

	@Override
	public GeneratorSetSizingResult calculate(GeneratorSetSizingInput input) {
		double phaseFactor = input.phaseType() == MotorPhaseType.THREE_PHASE ? SQRT_3 : 1.0;
		List<String> warnings = new ArrayList<>();

		List<LoadRow> rows = new ArrayList<>();
		rows.addAll(lightingRows(input.lightingAreas()));
		rows.addAll(receptacleRows(input.receptacleGroups(), input.applyReceptacleDemandFactor()));
		rows.addAll(otherRows(input.otherLoads()));
		MotorRows motorRows = motorRows(input, phaseFactor);
		rows.addAll(motorRows.rows());
		rows.addAll(directRows(input.directLoads(), phaseFactor, input.voltage()));
		boolean hasStartingLoads = !input.motors().isEmpty()
				|| input.directLoads().stream().anyMatch(load -> load.kind() == LoadKind.FIRE_PUMP);

		double connectedVa = rows.stream().mapToDouble(LoadRow::connectedVoltAmperes).sum();
		double demandVa = rows.stream().mapToDouble(LoadRow::demandVoltAmperes).sum();
		double connectedKva = connectedVa / 1000.0;
		double demandKva = demandVa / 1000.0;
		double demandCurrentAmps = demandVa / (phaseFactor * input.voltage());
		double runningRequiredKva = demandKva * (1.0 + input.spareCapacityPercent() / 100.0);

		// What is already running before any motor or fire pump starts; those are added step by step.
		double alwaysRunningDemandVa = rows.stream()
				.filter(row -> row.kind() != LoadKind.MOTOR && row.kind() != LoadKind.FIRE_PUMP)
				.mapToDouble(LoadRow::demandVoltAmperes)
				.sum();
		List<StartingStepDraft> startingDrafts = input.subtransientReactancePercent() == null || !hasStartingLoads
				? List.of()
				: startingSteps(input, phaseFactor, motorRows, alwaysRunningDemandVa, warnings);
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
							: voltageDipPercent(input.subtransientReactancePercent(), draft.startingKva(), selectedForDip);
					Boolean withinLimit = dip == null ? null : dip <= input.maximumVoltageDipPercent();
					return new StartingStep(draft.step(), draft.motors(), draft.startingKva(), draft.runningKvaBefore(),
							draft.requiredGeneratorKva(), dip, withinLimit);
				})
				.toList();
		String startingBasis = startingDrafts.isEmpty()
				? null
				: reference.lockedRotorBasis() + " " + reference.voltageDipBasis();

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
				startingBasis);
	}

	private record StartingStepDraft(int step, String motors, double startingKva, double runningKvaBefore, double requiredGeneratorKva) {
	}

	/** A motor or fire pump that starts in a step: what it adds to the running load, and its locked-rotor VA if known. */
	private record StartCandidate(String label, int step, double runningVa, Double startingVa) {
	}

	/**
	 * Locked-rotor kVA of the motors and fire pumps that start together, step by step, and the set each
	 * step needs to stay within the voltage dip limit. One with no locked-rotor current (a motor with no
	 * published value, a fire pump entered without one) is left out of its step with a warning, never
	 * silently.
	 */
	private List<StartingStepDraft> startingSteps(
			GeneratorSetSizingInput input, double phaseFactor, MotorRows motorRows, double alwaysRunningVa, List<String> warnings) {
		boolean polyphase = input.phaseType() == MotorPhaseType.THREE_PHASE;
		double reactance = input.subtransientReactancePercent() / 100.0;
		double dipLimit = input.maximumVoltageDipPercent() / 100.0;

		List<StartCandidate> candidates = new ArrayList<>();
		for (MotorEntry entry : motorRows.entries()) {
			MotorLoad motor = entry.motor();
			Double startingVa = null;
			try {
				double lockedRotorAmps = lockedRotorCalculator.calculate(
						new LockedRotorInput(polyphase, motor.horsepowerLabel(), motor.voltage())).lockedRotorAmps();
				startingVa = phaseFactor * motor.voltage() * lockedRotorAmps * motor.quantity() * motor.startingCurrentPercent() / 100.0;
			} catch (CalculationException e) {
				warnings.add(String.format(Locale.ROOT,
						"%s is left out of the starting check: no locked-rotor current is published for %s hp at %d V, so its starting is NOT included.",
						entry.label(), motor.horsepowerLabel(), motor.voltage()));
			}
			candidates.add(new StartCandidate(entry.label(), motor.startStep(), entry.runningVa(), startingVa));
		}
		for (DirectLoad load : input.directLoads()) {
			if (load.kind() != LoadKind.FIRE_PUMP) {
				continue;
			}
			String label = labelOf(load.label(), "Fire pump");
			Double startingVa = null;
			if (load.lockedRotorAmps() == null) {
				warnings.add(String.format(Locale.ROOT,
						"%s is left out of the starting check: no locked-rotor current was entered, so its starting is NOT included.", label));
			} else {
				startingVa = phaseFactor * input.voltage() * load.lockedRotorAmps() * load.startingCurrentPercent() / 100.0;
			}
			candidates.add(new StartCandidate(label, load.startStep(), directVoltAmperes(load, phaseFactor, input.voltage()), startingVa));
		}

		Map<Integer, List<StartCandidate>> byStep = new TreeMap<>();
		for (StartCandidate candidate : candidates) {
			byStep.computeIfAbsent(candidate.step(), step -> new ArrayList<>()).add(candidate);
		}

		List<StartingStepDraft> drafts = new ArrayList<>();
		double runningBeforeVa = alwaysRunningVa;
		for (Map.Entry<Integer, List<StartCandidate>> step : byStep.entrySet()) {
			double startingVa = 0;
			double stepRunningVa = 0;
			List<String> labels = new ArrayList<>();
			for (StartCandidate candidate : step.getValue()) {
				stepRunningVa += candidate.runningVa();
				if (candidate.startingVa() != null) {
					startingVa += candidate.startingVa();
					labels.add(candidate.label());
				}
			}
			if (!labels.isEmpty()) {
				double startingKva = startingVa / 1000.0;
				drafts.add(new StartingStepDraft(step.getKey(), String.join(", ", labels), startingKva, runningBeforeVa / 1000.0,
						reactance * startingKva * (1.0 / dipLimit - 1.0)));
			}
			runningBeforeVa += stepRunningVa;
		}
		return drafts;
	}

	/** The load in volt-amperes: kVA and kW (through the power factor) as given, amperes at the system voltage. */
	private static double directVoltAmperes(DirectLoad load, double phaseFactor, int voltage) {
		return switch (load.unit()) {
			case KVA -> load.amount() * 1000.0;
			case KW -> load.amount() * 1000.0 / load.powerFactor();
			case AMPS -> phaseFactor * voltage * load.amount();
		};
	}

	private List<LoadRow> directRows(List<DirectLoad> loads, double phaseFactor, int voltage) {
		List<LoadRow> rows = new ArrayList<>();
		for (DirectLoad load : loads) {
			double va = directVoltAmperes(load, phaseFactor, voltage);
			String entered = switch (load.unit()) {
				case KVA -> String.format(Locale.ROOT, "%s kVA", num(load.amount()));
				case KW -> String.format(Locale.ROOT, "%s kW at %.2f power factor = %s kVA", num(load.amount()),
						load.powerFactor(), num(va / 1000.0));
				case AMPS -> String.format(Locale.ROOT, "%s A at %d V = %s kVA", num(load.amount()), voltage, num(va / 1000.0));
			};
			boolean pump = load.kind() == LoadKind.FIRE_PUMP;
			String basis = pump
					? "Fire pump entered as a figure: " + entered + ", taken at 100%. The generator must carry it with every other load that runs at the same time (6.95.1.3(D)(1))."
					: "Total building load entered as a figure: " + entered + ", taken at 100%. Enter it after your own demand factors; none is applied here.";
			rows.add(new LoadRow(load.kind(), labelOf(load.label(), pump ? "Fire pump" : "Building load"),
					va, 100.0, va, load.continuous(), basis));
		}
		return rows;
	}

	/** Voltage dip, in percent, when {@code startingKva} is applied to a set of {@code generatorKva}. */
	private static double voltageDipPercent(double reactancePercent, double startingKva, double generatorKva) {
		double x = reactancePercent / 100.0;
		return x * startingKva / (generatorKva + x * startingKva) * 100.0;
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

	private List<LoadRow> lightingRows(List<LightingArea> areas) {
		record Resolved(LightingArea area, Occupancy occupancy, DemandGroup group, double unitLoad, double connectedVa, boolean fullDemand) {
		}
		List<Resolved> resolved = new ArrayList<>();
		for (LightingArea area : areas) {
			Occupancy occupancy = reference.occupancy(area.occupancy());
			AreaType areaType = reference.areaType(area.areaType());
			DemandGroup group = reference.demandGroup(occupancy.demandGroup());
			double tableUnitLoad = areaType.voltAmperesPerM2() != null ? areaType.voltAmperesPerM2() : occupancy.voltAmperesPerM2();
			double unitLoad = tableUnitLoad;
			if (area.unitLoadVoltAmperesPerM2() != null) {
				if (area.unitLoadVoltAmperesPerM2() < tableUnitLoad) {
					throw new CalculationException(String.format(Locale.ROOT,
							"The lighting unit load for %s cannot be below the Table 2.20.2.3 minimum of %s VA/m2 (2.20.2.3)",
							labelOf(area.label(), occupancy.label()), num(tableUnitLoad)));
				}
				unitLoad = area.unitLoadVoltAmperesPerM2();
			}
			boolean fullDemand = area.entireLightingUsedAtOnce() && group.fullDemandWhenAllLightingInUse();
			resolved.add(new Resolved(area, occupancy, group, unitLoad, area.floorAreaM2() * unitLoad, fullDemand));
		}

		// The stepped factors of Table 2.20.3.3 apply to the total lighting load of one occupancy
		// type, so areas of the same group are pooled before the tiers are walked.
		Map<String, Double> pooledVa = new LinkedHashMap<>();
		for (Resolved r : resolved) {
			if (!r.fullDemand()) {
				pooledVa.merge(r.group().key(), r.connectedVa(), Double::sum);
			}
		}
		Map<String, Double> effectivePercent = new LinkedHashMap<>();
		for (Map.Entry<String, Double> entry : pooledVa.entrySet()) {
			DemandGroup group = reference.demandGroup(entry.getKey());
			double demand = tierDemand(entry.getValue(), group.tiers());
			effectivePercent.put(entry.getKey(), demand / entry.getValue() * 100.0);
		}

		List<LoadRow> rows = new ArrayList<>();
		for (Resolved r : resolved) {
			double percent = r.fullDemand() ? 100.0 : effectivePercent.get(r.group().key());
			String basis = String.format(Locale.ROOT, "Table 2.20.2.3: %s VA/m2 x %s m2 (%s). ",
					num(r.unitLoad()), num(r.area().floorAreaM2()), r.occupancy().label());
			if (r.fullDemand()) {
				basis += "Table 2.20.3.3 note: the demand factors do not apply where the entire lighting is used at one time, so 100%.";
			} else if (r.group().tiers().size() == 1) {
				basis += "Table 2.20.3.3 (" + r.group().label() + "): no reduction, " + num(percent) + "%.";
			} else {
				basis += "Table 2.20.3.3 (" + r.group().label() + "): " + describeTiers(r.group().tiers())
						+ String.format(Locale.ROOT, "; %s%% on the %s VA of this occupancy.", num(percent), num(pooledVa.get(r.group().key())));
			}
			rows.add(new LoadRow(LoadKind.LIGHTING, labelOf(r.area().label(), r.occupancy().label()),
					r.connectedVa(), percent, r.connectedVa() * percent / 100.0, r.area().continuous(), basis));
		}
		return rows;
	}

	private List<LoadRow> receptacleRows(List<ReceptacleGroup> groups, boolean applyDemandFactor) {
		if (groups.isEmpty()) {
			return List.of();
		}
		double perOutlet = reference.receptacleVoltAmperesPerOutlet();
		double[] connected = new double[groups.size()];
		String[] basis = new String[groups.size()];
		double total = 0;
		for (int i = 0; i < groups.size(); i++) {
			ReceptacleGroup group = groups.get(i);
			double byOutlets = group.outlets() * perOutlet;
			connected[i] = byOutlets;
			basis[i] = String.format(Locale.ROOT, "2.20.2.5(I): %d outlets x %s VA.", group.outlets(), num(perOutlet));
			if (group.bankOrOfficeFloorAreaM2() != null) {
				double byArea = group.bankOrOfficeFloorAreaM2() * reference.bankOfficeReceptacleVoltAmperesPerM2();
				basis[i] = String.format(Locale.ROOT,
						"2.20.2.5(K): the larger of %d outlets x %s VA = %s VA and %s m2 x %s VA/m2 = %s VA.",
						group.outlets(), num(perOutlet), num(byOutlets), num(group.bankOrOfficeFloorAreaM2()),
						num(reference.bankOfficeReceptacleVoltAmperesPerM2()), num(byArea));
				connected[i] = Math.max(byOutlets, byArea);
			}
			total += connected[i];
		}
		double percent = 100.0;
		String demandText = " No demand factor applied (100%).";
		if (applyDemandFactor) {
			percent = tierDemand(total, reference.receptacleDemandTiers()) / total * 100.0;
			demandText = " Table 2.20.3.5: " + describeTiers(reference.receptacleDemandTiers())
					+ String.format(Locale.ROOT, "; %s%% on the %s VA of receptacle load.", num(percent), num(total));
		}
		List<LoadRow> rows = new ArrayList<>();
		for (int i = 0; i < groups.size(); i++) {
			ReceptacleGroup group = groups.get(i);
			rows.add(new LoadRow(LoadKind.RECEPTACLE, labelOf(group.label(), "Receptacle outlets"),
					connected[i], percent, connected[i] * percent / 100.0, group.continuous(), basis[i] + demandText));
		}
		return rows;
	}

	private List<LoadRow> otherRows(List<OtherLoad> loads) {
		List<LoadRow> rows = new ArrayList<>();
		for (OtherLoad load : loads) {
			rows.add(new LoadRow(LoadKind.OTHER, labelOf(load.label(), "Other load"),
					load.voltAmperes(), load.demandFactorPercent(),
					load.voltAmperes() * load.demandFactorPercent() / 100.0, load.continuous(),
					String.format(Locale.ROOT, "Entered load with a %s%% demand factor.", num(load.demandFactorPercent()))));
		}
		return rows;
	}

	/** A motor entry as the load schedule names it, with its running load, kept for the starting check. */
	private record MotorEntry(MotorLoad motor, String label, double runningVa) {
	}

	private record MotorRows(List<LoadRow> rows, double largestMotorVa, double totalMotorVa, List<MotorEntry> entries) {
	}

	private MotorRows motorRows(GeneratorSetSizingInput input, double phaseFactor) {
		boolean threePhase = input.phaseType() == MotorPhaseType.THREE_PHASE;
		List<LoadRow> rows = new ArrayList<>();
		List<MotorEntry> entries = new ArrayList<>();
		double largestVa = 0;
		double totalVa = 0;
		for (MotorLoad motor : input.motors()) {
			double flcAmps = motorFlcCalculator.calculate(new MotorFlcInput(
					input.phaseType(), threePhase ? MotorClass.INDUCTION : null,
					motor.horsepowerLabel(), motor.voltage(), null)).flcAmps();
			double eachVa = phaseFactor * motor.voltage() * flcAmps;
			double groupVa = eachVa * motor.quantity();
			largestVa = Math.max(largestVa, eachVa);
			totalVa += groupVa;
			String defaultLabel = motor.horsepowerLabel() + " hp motor";
			String label = labelOf(motor.label(), defaultLabel);
			if (motor.quantity() > 1) {
				label = motor.quantity() + " x " + label;
			}
			String basis = String.format(Locale.ROOT,
					"Table %s: %s hp at %d V draws %s A, %s kVA each; 100%% at full-load current (2.20.3.11, 4.30.2.4).",
					threePhase ? "4.30.14.4" : "4.30.14.2", motor.horsepowerLabel(), motor.voltage(), num(flcAmps), num(eachVa / 1000.0));
			// Not "continuous": a motor's duty is not asked for, and the 4.30.2.4 feeder treats motors by
			// their own rule (125% of the largest) rather than the continuous-load split of the others.
			rows.add(new LoadRow(LoadKind.MOTOR, label, groupVa, 100.0, groupVa, false, basis));
			entries.add(new MotorEntry(motor, label, groupVa));
		}
		return new MotorRows(rows, largestVa, totalVa, entries);
	}

	/** Walks a stepped demand factor: each tier's percent applies to the VA between the previous bound and its own. */
	private static double tierDemand(double totalVa, List<DemandTier> tiers) {
		double remaining = totalVa;
		double previousBound = 0;
		double demand = 0;
		for (DemandTier tier : tiers) {
			double bound = tier.upToVoltAmperes() == null ? Double.POSITIVE_INFINITY : tier.upToVoltAmperes();
			double portion = Math.min(remaining, bound - previousBound);
			demand += portion * tier.percent() / 100.0;
			remaining -= portion;
			previousBound = bound;
			if (remaining <= 0) {
				break;
			}
		}
		return demand;
	}

	private static String describeTiers(List<DemandTier> tiers) {
		List<String> parts = new ArrayList<>();
		double previousBound = 0;
		for (int i = 0; i < tiers.size(); i++) {
			DemandTier tier = tiers.get(i);
			if (tier.upToVoltAmperes() == null) {
				parts.add(i == 0
						? num(tier.percent()) + "% of the total"
						: num(tier.percent()) + "% of the remainder over " + num(previousBound) + " VA");
			} else {
				parts.add(i == 0
						? num(tier.percent()) + "% of the first " + num(tier.upToVoltAmperes()) + " VA"
						: num(tier.percent()) + "% from " + num(previousBound) + " to " + num(tier.upToVoltAmperes()) + " VA");
				previousBound = tier.upToVoltAmperes();
			}
		}
		return String.join(", ", parts);
	}

	private static String labelOf(String label, String fallback) {
		return label == null || label.isBlank() ? fallback : label.trim();
	}

	/** Whole numbers without a decimal point, anything else to one decimal place. */
	private static String num(double value) {
		return Math.abs(value - Math.rint(value)) < 1e-9
				? String.format(Locale.ROOT, "%,.0f", value)
				: String.format(Locale.ROOT, "%,.1f", value);
	}

}
