package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.Calculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Works out how many sprinklers make up the remote area and how they are arranged, the way a
 * designer does it by hand before drawing the network:
 *
 * <ol>
 *   <li>Take the design area, reduced for quick-response sprinklers where the ceiling allows it
 *       ({@link QuickResponseAreaReductionCalculator}).</li>
 *   <li>Lay it out as a rectangle whose side along the branch lines is {@code 1.2 * sqrt(A)}.</li>
 *   <li>Fit sprinklers along that side: {@code ceil(length / S)} per branch -- rounded up.</li>
 *   <li>Fit branches across: enough that {@code sprinklersPerBranch * branches} sprinklers cover
 *       the area.</li>
 *   <li>Find the first sprinkler's pressure: {@code (density * coverage / K)^2}, but never below
 *       the minimum operating pressure.</li>
 * </ol>
 *
 * <p>Rounding up is deliberate throughout: a partial sprinkler is a whole sprinkler, so the counts
 * never under-state the demand. The plan is therefore always at least the design area, and says by
 * how much.
 *
 * <p>This is a layout aid, not part of the hydraulic calculation: {@link SprinklerHydraulicCalculator}
 * takes whatever sprinklers the caller lists. The plan is what tells the caller how many to list.
 */
public class RemoteAreaPlanCalculator implements Calculator<RemoteAreaPlanInput, RemoteAreaPlanResult> {

	/**
	 * Floating point slack for the round-up. 50 / 10 can come out as 5.0000000000000001 and must
	 * not become six sprinklers.
	 */
	private static final double ROUNDING_EPSILON = 1e-9;

	private final RemoteAreaRulesResolver rulesResolver;
	private final QuickResponseAreaReductionCalculator reductionCalculator;

	public RemoteAreaPlanCalculator(
			RemoteAreaRulesResolver rulesResolver, QuickResponseAreaReductionCalculator reductionCalculator) {
		this.rulesResolver = rulesResolver;
		this.reductionCalculator = reductionCalculator;
	}

	@Override
	public RemoteAreaPlanResult calculate(RemoteAreaPlanInput input) {
		RemoteAreaRules rules = rulesResolver.rules();
		List<String> notes = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		// 1. Design area, reduced where quick-response sprinklers and the ceiling allow it.
		double reductionPercent = 0.0;
		boolean reductionApplied = false;
		double adjustedArea = input.designAreaSqFt();
		if (input.quickResponseSprinklers()) {
			QuickResponseAreaReductionResult reduction = reductionCalculator.calculate(
					new QuickResponseAreaReductionInput(
							input.designAreaSqFt(), input.ceilingHeightFt(), true));
			reductionPercent = reduction.reductionPercent();
			reductionApplied = reduction.reductionApplied();
			adjustedArea = reduction.adjustedDesignAreaSqFt();
			notes.addAll(reduction.notes());
		}

		// 2. The rectangle: length along the branch lines, then the width that makes up the area.
		double length = rules.remoteAreaLengthFactor() * Math.sqrt(adjustedArea);
		double width = adjustedArea / length;

		// 3 and 4. Whole sprinklers along a branch, then whole branches to cover the area.
		double coverage = input.sprinklerSpacingFt() * input.branchSpacingFt();
		int perBranch = roundUp(length / input.sprinklerSpacingFt());
		int branches = roundUp(adjustedArea / (perBranch * coverage));
		int total = perBranch * branches;
		double protectedArea = total * coverage;

		// 5. The first sprinkler: the pressure its share of the density needs, floored.
		double minimumFlow = input.densityGpmPerSqFt() * coverage;
		double pressureForFlow = SprinklerMath.pressurePsi(input.kFactor(), minimumFlow);
		double floor = rules.minimumOperatingPressurePsi();
		boolean floorGoverned = pressureForFlow < floor;
		double firstPressure = Math.max(pressureForFlow, floor);
		double firstFlow = SprinklerMath.flowGpm(input.kFactor(), firstPressure);

		if (floorGoverned) {
			notes.add("The " + format(minimumFlow) + " gpm needed over one sprinkler's coverage takes only "
					+ format(pressureForFlow) + " psi at K " + format(input.kFactor()) + ", below the "
					+ format(floor) + " psi minimum operating pressure -- so " + format(floor)
					+ " psi is used, and the sprinkler discharges " + format(firstFlow) + " gpm, more than the "
					+ "minimum.");
		}
		notes.add("Remote area " + format(length) + " ft along the branch lines by " + format(width) + " ft: "
				+ perBranch + " sprinklers per branch on " + branches + " branch lines is " + total
				+ " sprinklers, protecting " + format(protectedArea) + " ft2 against a design area of "
				+ format(adjustedArea) + " ft2. The counts round up, so the plan is never smaller than the "
				+ "design area.");

		if (input.maxCoveragePerSprinklerSqFt() != null && coverage > input.maxCoveragePerSprinklerSqFt()) {
			warnings.add("Each sprinkler would cover " + format(coverage) + " ft2, above the "
					+ format(input.maxCoveragePerSprinklerSqFt()) + " ft2 maximum for this hazard and construction "
					+ "-- reduce the spacing.");
		}
		if (!rules.isVerified()) {
			warnings.add("The remote area dimension (" + format(rules.remoteAreaLengthFactor()) + " x the square "
					+ "root of the area) and the " + format(floor) + " psi minimum operating pressure are "
					+ "reference data tagged '" + rules.confidence() + "' -- confirm both against the printed "
					+ "NFPA 13 before relying on this plan.");
		}

		return new RemoteAreaPlanResult(
				input.designAreaSqFt(),
				reductionPercent,
				reductionApplied,
				adjustedArea,
				length,
				width,
				coverage,
				perBranch,
				branches,
				total,
				protectedArea,
				minimumFlow,
				pressureForFlow,
				floor,
				firstPressure,
				firstFlow,
				floorGoverned,
				rules.confidence(),
				List.copyOf(notes),
				List.copyOf(warnings));
	}

	/** Whole sprinklers (or branches): anything left over is a full one, within float slack. */
	private static int roundUp(double value) {
		return Math.max(1, (int) Math.ceil(value - ROUNDING_EPSILON));
	}

	private static String format(double value) {
		String text = String.format(Locale.ROOT, "%.2f", value);
		return text.contains(".") ? text.replaceAll("0+$", "").replaceAll("\\.$", "") : text;
	}

}
