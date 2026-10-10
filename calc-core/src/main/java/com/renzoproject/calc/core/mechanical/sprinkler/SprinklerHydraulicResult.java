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
 * @param availablePressureAtDemandPsi what the supply curve delivers at {@code totalDemandFlowGpm}
 * @param safetyMarginPsi              available minus required; negative means the system fails
 * @param safetyMarginPercent          margin as a percentage of the available pressure
 * @param passes                       true when the supply meets or exceeds the requirement
 * @param supplyCurve                  sampled supply curve points, including the demand point
 * @param warnings                     advisory messages (velocity, thin margin, unverified
 *                                     reference data) -- never a substitute for {@code passes}
 */
public record SprinklerHydraulicResult(
		List<SegmentFlowRow> segments,
		double totalDemandFlowGpm,
		double requiredPressureAtSupplyPsi,
		double availablePressureAtDemandPsi,
		double safetyMarginPsi,
		double safetyMarginPercent,
		boolean passes,
		List<SupplyCurvePoint> supplyCurve,
		List<String> warnings) {

}
