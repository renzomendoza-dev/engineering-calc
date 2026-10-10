package com.renzoproject.calc_api.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.sprinkler.SprinklerHydraulicResult;

import java.util.List;

/**
 * HTTP response body for a sprinkler hydraulic calculation, mirroring calc-core's
 * {@link SprinklerHydraulicResult}. <b>Units:</b> psi, US gpm.
 *
 * @param segments                     calculation-sheet rows, most remote sprinkler first
 * @param totalDemandFlowGpm           demand at the supply point, including the hose allowance
 * @param requiredPressureAtSupplyPsi  pressure needed at the supply point, including device losses
 * @param availablePressureAtDemandPsi what the supply curve delivers at that demand
 * @param safetyMarginPsi              available minus required; negative means the design fails
 * @param safetyMarginPercent          margin as a percentage of the available pressure
 * @param passes                       whether the supply meets the requirement
 * @param supplyCurve                  sampled supply curve, including the demand point
 * @param warnings                     advisory messages; never a substitute for {@code passes}
 */
public record SprinklerHydraulicResponse(
		List<SegmentFlowRowDto> segments,
		Double totalDemandFlowGpm,
		Double requiredPressureAtSupplyPsi,
		Double availablePressureAtDemandPsi,
		Double safetyMarginPsi,
		Double safetyMarginPercent,
		Boolean passes,
		List<SupplyCurvePointDto> supplyCurve,
		List<String> warnings) {

	public static SprinklerHydraulicResponse from(SprinklerHydraulicResult result) {
		return new SprinklerHydraulicResponse(
				result.segments().stream().map(SegmentFlowRowDto::from).toList(),
				result.totalDemandFlowGpm(),
				result.requiredPressureAtSupplyPsi(),
				result.availablePressureAtDemandPsi(),
				result.safetyMarginPsi(),
				result.safetyMarginPercent(),
				result.passes(),
				result.supplyCurve().stream().map(SupplyCurvePointDto::from).toList(),
				result.warnings());
	}

}
