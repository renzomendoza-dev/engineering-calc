package com.renzoproject.calc.core.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.FirePumpSupply.FirePumpPlan;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorLoadSchedule.MotorEntry;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorLoadSchedule.MotorRows;
import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingInput.MotorLoad;
import com.renzoproject.calc.core.electrical.motorflc.LockedRotorCalculator;
import com.renzoproject.calc.core.electrical.motorflc.LockedRotorInput;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.exception.CalculationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The motor starting check of a generator set: the locked-rotor kVA of the motors and fire pump that
 * start together, step by step, the set each step needs to keep the voltage dip within its limit,
 * and the dip a chosen set gives. Part of {@link GeneratorSetSizingCalculator}.
 */
final class MotorStartingCheck {

	private final LockedRotorCalculator lockedRotorCalculator;

	MotorStartingCheck(LockedRotorCalculator lockedRotorCalculator) {
		this.lockedRotorCalculator = lockedRotorCalculator;
	}

	record StartingStepDraft(
			int step, String motors, double startingKva, double runningKvaBefore, double requiredGeneratorKva,
			double dipLimitPercent, boolean includesFirePump) {
	}

	/** A motor or fire pump that starts in a step: what it adds to the running load, and its locked-rotor VA if known. */
	record StartCandidate(String label, int step, double runningVa, Double startingVa, boolean firePump) {
	}

	/**
	 * Locked-rotor kVA of the motors and fire pumps that start together, step by step, and the set each
	 * step needs to stay within the voltage dip limit. One with no locked-rotor current (a motor with no
	 * published value, a fire pump entered without one) is left out of its step with a warning, never
	 * silently.
	 */
	List<StartingStepDraft> startingSteps(
			GeneratorSetSizingInput input, double phaseFactor, MotorRows motorRows, FirePumpPlan firePumpPlan,
			double alwaysRunningVa, List<String> warnings) {
		boolean polyphase = input.phaseType() == MotorPhaseType.THREE_PHASE;
		double reactance = input.subtransientReactancePercent() / 100.0;
		double generalDipLimitPercent = input.maximumVoltageDipPercent();

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
			candidates.add(new StartCandidate(entry.label(), motor.startStep(), entry.runningVa(), startingVa, false));
		}
		if (firePumpPlan != null) {
			if (firePumpPlan.pumpStartingVa() == null) {
				warnings.add("The fire pump is left out of the starting check: no locked-rotor current is available for it, so its starting is NOT included.");
			}
			candidates.add(new StartCandidate("Fire pump", input.firePump().startStep(), firePumpPlan.pumpRunningVa(),
					firePumpPlan.pumpStartingVa(), true));
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
			boolean includesFirePump = false;
			List<String> labels = new ArrayList<>();
			for (StartCandidate candidate : step.getValue()) {
				stepRunningVa += candidate.runningVa();
				if (candidate.startingVa() != null) {
					startingVa += candidate.startingVa();
					labels.add(candidate.label());
					includesFirePump |= candidate.firePump();
				}
			}
			if (!labels.isEmpty()) {
				double startingKva = startingVa / 1000.0;
				// A step with the fire pump is held to Article 6.95's limit, less what the cable takes, if
				// that is tighter than the limit entered for motors in general.
				double dipLimitPercent = includesFirePump
						? Math.min(generalDipLimitPercent, firePumpPlan.generatorDipLimitPercent())
						: generalDipLimitPercent;
				drafts.add(new StartingStepDraft(step.getKey(), String.join(", ", labels), startingKva, runningBeforeVa / 1000.0,
						reactance * startingKva * (100.0 / dipLimitPercent - 1.0), dipLimitPercent, includesFirePump));
			}
			runningBeforeVa += stepRunningVa;
		}
		return drafts;
	}

	/** Voltage dip, in percent, when {@code startingKva} is applied to a set of {@code generatorKva}. */
	static double voltageDipPercent(double reactancePercent, double startingKva, double generatorKva) {
		double x = reactancePercent / 100.0;
		return x * startingKva / (generatorKva + x * startingKva) * 100.0;
	}

}
