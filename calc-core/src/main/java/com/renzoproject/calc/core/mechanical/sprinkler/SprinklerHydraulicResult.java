package com.renzoproject.calc.core.mechanical.sprinkler;

import java.util.List;

/**
 * Result of {@link SprinklerHydraulicCalculator}. <b>Units:</b> psi, US gpm.
 *
 * @param segments                     calculation-sheet rows, ordered most remote sprinkler first
 * @param totalDemandFlowGpm           system demand at the supply point, including the hose stream
 *                                     allowance
 * @param requiredPressureAtSupplyPsi  pressure the system needs at the supply point, including
 *                                     device losses
 * @param availablePressureAtDemandPsi what the supply delivers at {@code totalDemandFlowGpm};
 *                                     <b>null</b> when the input carried no water supply
 * @param safetyMarginPsi              available minus required; negative means the system fails.
 *                                     <b>Null</b> when there is no supply to compare against
 * @param safetyMarginPercent          margin as a percentage of the available pressure;
 *                                     <b>null</b> when there is no supply
 * @param passes                       true when the supply meets or exceeds the requirement;
 *                                     <b>null</b> when there is no supply -- not {@code false},
 *                                     which would read as a failed system rather than an
 *                                     unanswered question
 * @param supplyCurve                  sampled supply curve points, including the demand point;
 *                                     empty when there is no supply
 * @param warnings                     advisory messages (velocity, thin margin, unverified
 *                                     reference data) -- never a substitute for {@code passes}
 * @param identicalBranches            one line for each branch declared identical to another: what
 *                                     it was scaled from and to. Empty when none were declared. Its
 *                                     flow is already in {@code totalDemandFlowGpm}; the pipes it
 *                                     stands for have no rows of their own in {@code segments}
 */
public record SprinklerHydraulicResult(
		List<SegmentFlowRow> segments,
		double totalDemandFlowGpm,
		double requiredPressureAtSupplyPsi,
		Double availablePressureAtDemandPsi,
		Double safetyMarginPsi,
		Double safetyMarginPercent,
		Boolean passes,
		List<SupplyCurvePoint> supplyCurve,
		List<String> warnings,
		List<IdenticalBranchRow> identicalBranches) {

	/** A result for a network with no identical branches. */
	public SprinklerHydraulicResult(
			List<SegmentFlowRow> segments,
			double totalDemandFlowGpm,
			double requiredPressureAtSupplyPsi,
			Double availablePressureAtDemandPsi,
			Double safetyMarginPsi,
			Double safetyMarginPercent,
			Boolean passes,
			List<SupplyCurvePoint> supplyCurve,
			List<String> warnings) {
		this(segments, totalDemandFlowGpm, requiredPressureAtSupplyPsi, availablePressureAtDemandPsi,
				safetyMarginPsi, safetyMarginPercent, passes, supplyCurve, warnings, List.of());
	}

	/** True when a supply was given, so the margin and verdict carry an answer. */
	public boolean hasSupply() {
		return passes != null;
	}

}
