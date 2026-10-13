package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.Calculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Works out the remote area the way a designer does by hand before drawing the network:
 *
 * <ol>
 *   <li>Take the design area, reduced for quick-response sprinklers where the ceiling allows it
 *       ({@link QuickResponseAreaReductionCalculator}).</li>
 *   <li>Each sprinkler covers a set area, so it must flow {@code density * coverage}, and the
 *       fewest sprinklers that protect the area is {@code ceil(area / coverage)}.</li>
 *   <li>The first sprinkler's pressure is {@code (flow / K)^2}, but never below the minimum
 *       operating pressure.</li>
 *   <li>Lay the area out as a rectangle whose side along the branch lines is
 *       {@code 1.2 * sqrt(A)}.</li>
 *   <li>With the spacing along the branch: {@code ceil(length / S)} sprinklers per branch, and
 *       enough branches that {@code perBranch * branches} sprinklers cover the area.</li>
 * </ol>
 *
 * <p>The steps are independent tiers, so a form can show each as soon as its inputs exist: the
 * first three need no spacing, and the layout counts are left {@code null} until it is given.
 *
 * <p>Rounding up is deliberate throughout: a partial sprinkler is a whole sprinkler, so the counts
 * never under-state the demand.
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

		// 2. What each sprinkler flows, and the fewest that protect the area.
		double coverage = input.coveragePerSprinklerSqFt();
		double flowPerSprinkler = input.densityGpmPerSqFt() * coverage;
		int sprinklersForArea = roundUp(adjustedArea / coverage);

		// 3. The first sprinkler: the pressure its flow needs, floored.
		double pressureForFlow = SprinklerMath.pressurePsi(input.kFactor(), flowPerSprinkler);
		double standardFloor = rules.minimumOperatingPressurePsi();
		// A listing may specify more than the standard's minimum (27.2.4.11.2); it can only raise it.
		double listedMinimum = input.listedMinimumPressurePsi() == null ? 0.0 : input.listedMinimumPressurePsi();
		double floor = Math.max(standardFloor, listedMinimum);
		boolean listingGoverns = listedMinimum > standardFloor;
		boolean floorGoverned = pressureForFlow < floor;
		double firstPressure = Math.max(pressureForFlow, floor);
		double firstFlow = SprinklerMath.flowGpm(input.kFactor(), firstPressure);

		// 4. The rectangle: length along the branch lines, then the width that makes up the area.
		double length = rules.remoteAreaLengthFactor() * Math.sqrt(adjustedArea);
		double width = adjustedArea / length;

		// 5. The layout, once the spacing along the branch is known.
		Double branchSpacing = null;
		Integer perBranch = null;
		Integer branches = null;
		Integer total = null;
		Double protectedArea = null;
		if (input.sprinklerSpacingFt() != null) {
			double spacing = input.sprinklerSpacingFt();
			branchSpacing = coverage / spacing;
			perBranch = roundUp(length / spacing);
			branches = roundUp(adjustedArea / (perBranch * coverage));
			total = perBranch * branches;
			protectedArea = total * coverage;
			notes.add("Remote area " + format(length) + " ft along the branch lines by " + format(width)
					+ " ft: " + perBranch + " sprinklers per branch on " + branches + " branch lines is " + total
					+ " sprinklers, protecting " + format(protectedArea) + " ft2 against a design area of "
					+ format(adjustedArea) + " ft2. The counts round up, so the plan is never smaller than the "
					+ "design area.");
		}

		if (perBranch != null) {
			notes.add("NFPA 13 27.2.4.2.1 asks for a rectangular area whose dimension along the branch lines is at "
					+ "least " + format(rules.remoteAreaLengthFactor()) + " x the square root of the area, "
					+ "laid out so that it permits sprinklers on both sides of the cross main.");
		}
		if (listingGoverns) {
			notes.add("The sprinkler listing's minimum operating pressure of " + format(listedMinimum)
					+ " psi applies instead of the " + format(standardFloor) + " psi NFPA 13 sets for any "
					+ "sprinkler (27.2.4.11.2).");
		}

		if (floorGoverned) {
			notes.add("The " + format(flowPerSprinkler) + " gpm needed over one sprinkler's coverage takes only "
					+ format(pressureForFlow) + " psi at K " + format(input.kFactor()) + ", below the "
					+ format(floor) + " psi minimum operating pressure -- so " + format(floor)
					+ " psi is used, and the sprinkler discharges " + format(firstFlow) + " gpm, more than the "
					+ "minimum.");
		}

		if (input.maxCoveragePerSprinklerSqFt() != null && coverage > input.maxCoveragePerSprinklerSqFt()) {
			warnings.add("Each sprinkler would cover " + format(coverage) + " ft2, above the "
					+ format(input.maxCoveragePerSprinklerSqFt()) + " ft2 maximum for this hazard and construction "
					+ "-- reduce the coverage.");
		}
		// Say only what is still open: a rule that has been checked against the printed standard is
		// not asked to be checked again.
		if (!rules.isRemoteAreaLengthVerified()) {
			warnings.add("The remote area dimension (" + format(rules.remoteAreaLengthFactor()) + " x the square "
					+ "root of the area) is reference data tagged '" + rules.remoteAreaLengthConfidence()
					+ "' -- confirm it against the printed NFPA 13 before relying on this plan.");
		}
		if (!rules.isMinimumOperatingPressureVerified()) {
			warnings.add("The " + format(standardFloor) + " psi minimum operating pressure is reference data "
					+ "tagged '" + rules.minimumOperatingPressureConfidence() + "' -- confirm it against the "
					+ "printed NFPA 13 before relying on this plan.");
		}

		return new RemoteAreaPlanResult(
				input.designAreaSqFt(),
				reductionPercent,
				reductionApplied,
				adjustedArea,
				coverage,
				sprinklersForArea,
				flowPerSprinkler,
				pressureForFlow,
				floor,
				firstPressure,
				firstFlow,
				floorGoverned,
				length,
				width,
				branchSpacing,
				perBranch,
				branches,
				total,
				protectedArea,
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
