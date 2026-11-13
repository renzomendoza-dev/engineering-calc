package com.renzoproject.calc.core.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.FirePump;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.PumpMotor;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.SupplyRun;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.FirePumpConductor;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.FirePumpResult;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.LoadRow;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.Requirement;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingResult.StartingStep;
import com.renzoproject.calc.core.electrical.generatorset.MotorStartingCheck.StartingStepDraft;
import com.renzoproject.calc.core.electrical.motorflc.LockedRotorCalculator;
import com.renzoproject.calc.core.electrical.motorflc.LockedRotorInput;
import com.renzoproject.calc.core.electrical.motorflc.MotorFlcCalculator;
import com.renzoproject.calc.core.electrical.motorflc.MotorFlcInput;
import com.renzoproject.calc.core.electrical.reference.AmbientTempCorrectionTable;
import com.renzoproject.calc.core.electrical.reference.AmpacityTable;
import com.renzoproject.calc.core.electrical.reference.ConductorProperties;
import com.renzoproject.calc.core.electrical.reference.ConductorPropertiesResolver;
import com.renzoproject.calc.core.electrical.reference.ConductorSize;
import com.renzoproject.calc.core.electrical.reference.InsulationTypeTempRating;
import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropCalculator;
import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropInput;
import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropResult;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingCalculator;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingInput;
import com.renzoproject.calc.core.electrical.wiresizing.WireSizingResult;
import com.renzoproject.calc.core.exception.CalculationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.renzoproject.calc.core.electrical.generatorset.GeneratorSetText.num;

/**
 * The fire pump on the generator set under PEC 2017 Article 6.95: the pump, pressure maintenance pump
 * and accessory loads, the supply conductors of 6.95.1.6(B) and their voltage drops, and the
 * controller voltage checks of 6.95.1.7 once the set is chosen. Part of
 * {@link GeneratorSetSizingCalculator}.
 */
final class FirePumpSupply {

	private final GeneratorSetReference reference;
	private final MotorFlcCalculator motorFlcCalculator;
	private final LockedRotorCalculator lockedRotorCalculator;
	private final WireSizingCalculator wireSizingCalculator;
	private final AmpacityTable ampacityTable = new AmpacityTable();
	private final InsulationTypeTempRating insulationTypeTempRating = new InsulationTypeTempRating();
	private final AmbientTempCorrectionTable ambientTempCorrectionTable = new AmbientTempCorrectionTable();
	private final ConductorPropertiesResolver conductorPropertiesResolver = new ConductorPropertiesResolver();
	private final VoltageDropCalculator voltageDropCalculator = new VoltageDropCalculator();

	FirePumpSupply(GeneratorSetReference reference, MotorFlcCalculator motorFlcCalculator,
			LockedRotorCalculator lockedRotorCalculator, WireSizingCalculator wireSizingCalculator) {
		this.reference = reference;
		this.motorFlcCalculator = motorFlcCalculator;
		this.lockedRotorCalculator = lockedRotorCalculator;
		this.wireSizingCalculator = wireSizingCalculator;
	}

	/** One pump's currents at the system voltage, and where they came from. */
	record PumpFigures(double fullLoadAmps, Double lockedRotorAmps, String source) {
	}

	/** Everything Article 6.95 needs worked out before the set is chosen: the loads, the conductors, the cable drops. */
	record FirePumpPlan(
			List<LoadRow> rows,
			double pumpAmps,
			Double jockeyAmps,
			Double accessoryAmps,
			Double pumpLockedRotorAmps,
			double pumpRunningVa,
			Double pumpStartingVa,
			double alwaysRunningVa,
			double minimumConductorAmpacityAmps,
			String conductorBasis,
			FirePumpConductor conductor,
			Double cableStartingDropPercent,
			Double cableRunningDropPercent,
			double generatorDipLimitPercent) {
	}

	record RunSizing(FirePumpConductor conductor, Double startingDropPercent, Double runningDropPercent) {
	}

	/**
	 * The currents of a pump at the system voltage: from the PEC horsepower tables (the motor's own
	 * voltage converted to the system voltage through the volt-amperes) or as entered.
	 */
	PumpFigures pumpFigures(PumpMotor pump, GeneratorSetSizingInput input, double phaseFactor, boolean needLockedRotor,
			String name, List<String> warnings) {
		if (!pump.byHorsepower()) {
			return new PumpFigures(pump.fullLoadAmps(), pump.lockedRotorAmps(),
					String.format(Locale.ROOT, "Entered full-load current %s A at %d V", num(pump.fullLoadAmps()), input.voltage()));
		}
		boolean threePhase = input.phaseType() == MotorPhaseType.THREE_PHASE;
		double flc = motorFlcCalculator.calculate(new MotorFlcInput(
				input.phaseType(), threePhase ? MotorClass.INDUCTION : null, pump.horsepowerLabel(), pump.voltage(), null)).flcAmps();
		double toSystemVoltage = (double) pump.voltage() / input.voltage();
		Double lockedRotorAmps = null;
		if (needLockedRotor) {
			try {
				lockedRotorAmps = lockedRotorCalculator.calculate(
						new LockedRotorInput(threePhase, pump.horsepowerLabel(), pump.voltage())).lockedRotorAmps() * toSystemVoltage;
			} catch (CalculationException e) {
				warnings.add(String.format(Locale.ROOT,
						"No locked-rotor current is published for the %s at %s hp and %d V, so its starting is NOT included.",
						name, pump.horsepowerLabel(), pump.voltage()));
			}
		}
		String source = String.format(Locale.ROOT, "Table %s: %s hp at %d V draws %s A",
				threePhase ? "4.30.14.4" : "4.30.14.2", pump.horsepowerLabel(), pump.voltage(), num(flc));
		return new PumpFigures(flc * toSystemVoltage, lockedRotorAmps, source);
	}

	FirePumpPlan firePumpPlan(GeneratorSetSizingInput input, double phaseFactor, List<String> warnings) {
		FirePump pump = input.firePump();
		double vaPerAmp = phaseFactor * input.voltage();

		PumpFigures motor = pumpFigures(pump.motor(), input, phaseFactor, true, "fire pump motor", warnings);
		PumpFigures jockey = pump.jockey() == null ? null : pumpFigures(pump.jockey(), input, phaseFactor, false, "pressure maintenance pump", warnings);
		Double accessoryAmps = pump.accessoryAmps() == null || pump.accessoryAmps() == 0 ? null : pump.accessoryAmps();

		double pumpVa = vaPerAmp * motor.fullLoadAmps();
		double jockeyVa = jockey == null ? 0 : vaPerAmp * jockey.fullLoadAmps();
		double accessoryVa = accessoryAmps == null ? 0 : vaPerAmp * accessoryAmps;

		String carry = " Counted at 100%: the generator must carry the fire pump with every other load that runs at the same time (6.95.1.3(D)(1)).";
		List<LoadRow> rows = new ArrayList<>();
		rows.add(new LoadRow(LoadKind.FIRE_PUMP, "Fire pump motor", pumpVa, 100.0, pumpVa, false,
				motor.source() + ", " + num(pumpVa / 1000.0) + " kVA." + carry));
		if (jockey != null) {
			rows.add(new LoadRow(LoadKind.FIRE_PUMP, "Pressure maintenance pump", jockeyVa, 100.0, jockeyVa, false,
					jockey.source() + ", " + num(jockeyVa / 1000.0) + " kVA. Taken as running before the fire pump starts."));
		}
		if (accessoryAmps != null) {
			rows.add(new LoadRow(LoadKind.FIRE_PUMP, "Fire pump accessories", accessoryVa, 100.0, accessoryVa, false,
					String.format(Locale.ROOT, "Entered full-load current %s A at %d V, %s kVA. Taken as running before the fire pump starts.",
							num(accessoryAmps), input.voltage(), num(accessoryVa / 1000.0))));
		}

		double pumpPercent = pump.startingCurrentPercent();
		Double pumpStartingVa = motor.lockedRotorAmps() == null ? null : vaPerAmp * motor.lockedRotorAmps() * pumpPercent / 100.0;

		double jockeyAmps = jockey == null ? 0 : jockey.fullLoadAmps();
		double accessory = accessoryAmps == null ? 0 : accessoryAmps;
		double factor = reference.firePumpConductorFactor();
		double minimumAmpacity = factor * (motor.fullLoadAmps() + jockeyAmps) + accessory;
		String conductorBasis = jockey == null && accessoryAmps == null
				? String.format(Locale.ROOT, "6.95.1.6(B)(2) with 4.30.2.2: 125%% of the fire pump motor's full-load current %s A = %s A.",
						num(motor.fullLoadAmps()), num(minimumAmpacity))
				: String.format(Locale.ROOT,
						"6.95.1.6(B)(1): 125%% of the fire pump and pressure maintenance full-load currents (%s A + %s A) + 100%% of the accessories (%s A) = %s A.",
						num(motor.fullLoadAmps()), num(jockeyAmps), num(accessory), num(minimumAmpacity));

		RunSizing run = pump.supplyRun() == null
				? new RunSizing(null, null, null)
				: sizeFirePumpRun(input, pump, motor, jockeyAmps, accessory, minimumAmpacity, phaseFactor, warnings);

		double maxStartingDrop = reference.firePumpMaxStartingDropPercent();
		double generatorLimit = maxStartingDrop;
		if (run.startingDropPercent() != null) {
			generatorLimit = maxStartingDrop - run.startingDropPercent();
			if (generatorLimit < 1.0) {
				// No generator can make up for a cable that takes the whole allowance, so the set is sized as
				// if the cable took only its share (the result still shows the cable's real drop, over the limit).
				generatorLimit = maxStartingDrop - pump.supplyRun().cableStartingDropAllowancePercent();
				warnings.add(String.format(Locale.ROOT,
						"The cable to the fire pump controller alone drops %s%% at starting, which leaves the generator almost none of the %s%% allowed (6.95.1.7). The set is sized as if the cable took only its %s%% share; shorten the run or use more or larger conductors.",
						num(run.startingDropPercent()), num(maxStartingDrop),
						num(pump.supplyRun().cableStartingDropAllowancePercent())));
			}
		}

		return new FirePumpPlan(rows, motor.fullLoadAmps(), jockey == null ? null : jockey.fullLoadAmps(), accessoryAmps,
				motor.lockedRotorAmps(), pumpVa, pumpStartingVa, jockeyVa + accessoryVa, minimumAmpacity, conductorBasis,
				run.conductor(), run.startingDropPercent(), run.runningDropPercent(), generatorLimit);
	}

	/**
	 * Sizes the run to the fire pump controller: the smallest conductor that carries the 6.95.1.6(B)
	 * ampacity and, when the run's length is given, keeps the running drop within 5% and the cable's
	 * share of the starting drop within the allowance entered. If no size does, the size the ampacity
	 * alone needs is used and the warning says so.
	 */
	RunSizing sizeFirePumpRun(GeneratorSetSizingInput input, FirePump pump, PumpFigures motor, double jockeyAmps,
			double accessoryAmps, double minimumAmpacity, double phaseFactor, List<String> warnings) {
		SupplyRun run = pump.supplyRun();
		// Passed as a non-continuous load: the 125% of 6.95.1.6(B) is already in minimumAmpacity.
		WireSizingResult base;
		try {
			base = wireSizingCalculator.calculate(new WireSizingInput(minimumAmpacity, false, run.ambientTempCelsius(),
					run.numberOfCurrentCarryingConductors(), run.numberOfParallelSets(), run.insulationType(),
					run.conductorMaterial(), run.terminationTempRatingCelsius(), null));
		} catch (CalculationException e) {
			warnings.add("Fire pump conductors not sized: " + e.getMessage() + ".");
			return new RunSizing(null, null, null);
		}

		Double startingCurrent = motor.lockedRotorAmps() == null
				? null
				: motor.lockedRotorAmps() * pump.startingCurrentPercent() / 100.0 + jockeyAmps + accessoryAmps;
		double runningCurrent = reference.firePumpRunningCurrentFactor() * motor.fullLoadAmps();
		double runningLimit = reference.firePumpMaxRunningDropPercent();

		String chosenLabel = base.recommendedSizeLabel();
		Double startDrop = null;
		Double runDrop = null;
		if (run.oneWayLengthMeters() != null) {
			List<ConductorSize> sizes = ampacityTable.allSizesSortedAscendingByArea();
			int from = 0;
			for (int i = 0; i < sizes.size(); i += 1) {
				if (sizes.get(i).label().equals(base.recommendedSizeLabel())) {
					from = i;
					break;
				}
			}
			boolean found = false;
			for (int i = from; i < sizes.size() && !found; i += 1) {
				ConductorSize candidate = sizes.get(i);
				Double candidateRun = cableDropPercent(input, run, candidate, runningCurrent, run.runningPowerFactor(), phaseFactor);
				Double candidateStart = startingCurrent == null
						? null
						: cableDropPercent(input, run, candidate, startingCurrent, run.startingPowerFactor(), phaseFactor);
				if (candidateRun == null || (startingCurrent != null && candidateStart == null)) {
					continue;
				}
				if (candidateRun <= runningLimit
						&& (candidateStart == null || candidateStart <= run.cableStartingDropAllowancePercent())) {
					chosenLabel = candidate.label();
					runDrop = candidateRun;
					startDrop = candidateStart;
					found = true;
				}
			}
			if (!found) {
				ConductorSize baseSize = sizes.get(from);
				runDrop = cableDropPercent(input, run, baseSize, runningCurrent, run.runningPowerFactor(), phaseFactor);
				startDrop = startingCurrent == null
						? null
						: cableDropPercent(input, run, baseSize, startingCurrent, run.startingPowerFactor(), phaseFactor);
				warnings.add(String.format(Locale.ROOT,
						"No conductor size keeps the fire pump run within %s%% running and %s%% of the starting drop for the cable; the %s mm2 the ampacity needs is shown. Shorten the run or use more parallel sets.",
						num(runningLimit), num(run.cableStartingDropAllowancePercent()), base.recommendedSizeLabel()));
			}
		}

		boolean upsized = !chosenLabel.equals(base.recommendedSizeLabel());
		double derated = base.deratedAmpacityAmps();
		boolean meetsTermination = base.meetsTerminationRating();
		if (upsized) {
			int insulationRating = insulationTypeTempRating.lookup(run.insulationType(), run.conductorMaterial());
			derated = ampacityTable.lookup(run.conductorMaterial(), chosenLabel, insulationRating)
					* base.tempCorrectionFactor() * base.adjustmentFactor();
			double terminationDerated = ampacityTable.lookup(run.conductorMaterial(), chosenLabel, run.terminationTempRatingCelsius())
					* ambientTempCorrectionTable.lookup(run.ambientTempCelsius(), run.terminationTempRatingCelsius())
					* base.adjustmentFactor();
			meetsTermination = terminationDerated >= base.requiredAmpacityPerSetAmps();
		}
		return new RunSizing(
				new FirePumpConductor(chosenLabel, base.recommendedSizeLabel(), upsized, run.numberOfParallelSets(),
						base.requiredAmpacityPerSetAmps(), derated, meetsTermination),
				startDrop, runDrop);
	}

	/** Voltage drop of the run for one size, as a percent of the system voltage; {@code null} if the size has no impedance data. */
	Double cableDropPercent(GeneratorSetSizingInput input, SupplyRun run, ConductorSize size, double currentAmps,
			double powerFactor, double phaseFactor) {
		try {
			CircuitType circuit = phaseFactor > 1.0 ? CircuitType.THREE_PHASE_AC : CircuitType.SINGLE_PHASE_AC;
			ConductorProperties properties = conductorPropertiesResolver.resolve(circuit, size, run.conductorMaterial(), run.conduitMaterial());
			VoltageDropResult result = voltageDropCalculator.calculate(new VoltageDropInput(circuit, currentAmps,
					run.oneWayLengthMeters(), properties.resistanceOhmsPerMeter(), properties.reactanceOhmsPerMeter(),
					powerFactor, input.voltage(), run.numberOfParallelSets()));
			return result.voltageDropPercent();
		} catch (CalculationException e) {
			return null;
		}
	}

	FirePumpResult firePumpResult(FirePumpPlan plan, List<StartingStepDraft> drafts, List<StartingStep> steps,
			Double nameplateCurrentAmps) {
		Double generatorDrop = null;
		for (int i = 0; i < drafts.size(); i += 1) {
			if (drafts.get(i).includesFirePump()) {
				generatorDrop = steps.get(i).voltageDipPercentAtSelected();
			}
		}
		Double controllerDrop = generatorDrop == null
				? null
				: generatorDrop + (plan.cableStartingDropPercent() == null ? 0.0 : plan.cableStartingDropPercent());
		Boolean startingWithin = controllerDrop == null ? null : controllerDrop <= reference.firePumpMaxStartingDropPercent();
		Boolean runningWithin = plan.cableRunningDropPercent() == null
				? null
				: plan.cableRunningDropPercent() <= reference.firePumpMaxRunningDropPercent();
		Double generatorPercent = nameplateCurrentAmps == null ? null : nameplateCurrentAmps / plan.pumpAmps() * 100.0;
		Boolean exceeds = generatorPercent == null ? null : generatorPercent > reference.firePumpGeneratorExceptionPercent();
		String voltageDropBasis = reference.firePumpVoltageDropSource()
				+ " The cable's drops come from the conductor tables with the power factors entered: at starting with the pump's"
				+ " locked-rotor current plus the pressure maintenance and accessory currents, running with 115% of the pump's"
				+ " full-load current. The starting drop at the controller is the generator's dip plus the cable's, added;"
				+ " generator regulation is not part of the running drop. Without a run length only the generator's dip is counted.";
		return new FirePumpResult(
				plan.pumpAmps(),
				plan.jockeyAmps(),
				plan.accessoryAmps(),
				plan.pumpLockedRotorAmps(),
				plan.minimumConductorAmpacityAmps(),
				plan.conductorBasis(),
				plan.conductor(),
				plan.cableStartingDropPercent(),
				plan.cableRunningDropPercent(),
				runningWithin,
				generatorDrop,
				controllerDrop,
				startingWithin,
				generatorPercent,
				exceeds,
				voltageDropBasis,
				reference.firePumpRequirements().stream()
						.map(entry -> new Requirement(entry.citation(), entry.text()))
						.toList());
	}

}
