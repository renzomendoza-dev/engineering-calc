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
 * @param availablePressureAtDemandPsi what the supply delivers at that demand; <b>null</b> when
 *                                     the request carried no water supply
 * @param safetyMarginPsi              available minus required; negative means the design fails.
 *                                     <b>Null</b> when there is no supply
 * @param safetyMarginPercent          margin as a percentage of the available pressure;
 *                                     <b>null</b> when there is no supply
 * @param passes                       whether the supply meets the requirement; <b>null</b> when
 *                                     there is no supply -- not false, which would read as a
 *                                     failed system rather than a question nobody asked
 * @param supplyCurve                  sampled supply curve, including the demand point; empty
 *                                     when there is no supply
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
