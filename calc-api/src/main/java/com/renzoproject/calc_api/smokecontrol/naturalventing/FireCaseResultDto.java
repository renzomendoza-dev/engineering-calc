package com.renzoproject.calc_api.smokecontrol.naturalventing;

import java.util.List;

/**
 * Result for one fire case, mirroring calc-core's {@code NaturalVentingResult.FireCaseResult}.
 * <b>Units:</b> m, m2, kW, kW/m2, K, kg/s, kg/m3, m/s. {@code regime} is {@code BELOW_LAYER}
 * (Equation 9.2.3.6) or {@code REACHES_LAYER} (9.2.3.7).
 */
public record FireCaseResultDto(
		String name,
		String zoneName,
		double designHeatReleaseRate,
		boolean heatReleaseRateCapped,
		Double growthCoefficient,
		double fireDiameter,
		double meanFlameHeight,
		double virtualOrigin,
		double layerHeightAboveFire,
		double layerDepth,
		String regime,
		double lineFireFactor,
		double convectiveHeatReleaseRate,
		double plumeMassFlow,
		double layerTemperatureRise,
		double layerTemperature,
		double layerDensity,
		double aerodynamicVentArea,
		Double requiredVentArea,
		Double inletCorrection,
		Double requiredInletArea,
		double inletAreaForVelocityLimit,
		Double inletVelocity,
		double floorRadiantFlux,
		double feasibleHeatReleaseRate,
		Double minimumAisleWidth,
		List<VentingCheckDto> checks) {

}
