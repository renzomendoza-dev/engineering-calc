package com.renzoproject.calc_api.smokecontrol;

/**
 * HTTP response body for a t-squared fire growth smoke production calculation. Mirrors calc-core's
 * {@code TSquaredSmokeProductionResult} exactly -- every intermediate value is exposed, not just
 * {@code volumetricFlowRate}. Deliberately separate from {@link SmokeProductionResponse} --
 * matching the "fully separate, duplicated" decision already made at the core layer.
 *
 * <p>{@code smokeTemperature} is degC, passed straight through: calc-core's
 * {@code TSquaredSmokeProductionResult.smokeTemperature()} is already Celsius -- the
 * Kelvin-relative {@code (Ts + 273)} step in the core calculator is local to the density formula
 * only and never appears in the returned temperature itself, so no unit conversion happens here.
 */
public record TSquaredSmokeProductionResponse(
		Double evaluationTime,
		Double designHeatReleaseRate,
		boolean isGrowthCapped,
		Double convectiveHeatReleaseRate,
		Double flameHeight,
		Double heightAboveFire,
		TSquaredPlumeRegimeDto plumeRegime,
		Double smokeTemperature,
		Double smokeDensity,
		Double volumetricFlowRate) {

}
