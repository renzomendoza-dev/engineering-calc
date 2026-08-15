package com.renzoproject.calc_api.smokecontrol;

/**
 * HTTP response body for a smoke production (plume) calculation. Mirrors calc-core's
 * {@code SmokeProductionResult} exactly — every intermediate value is exposed, not just
 * {@code volumetricFlowRate}, matching that Result record's own design (the eventual web layer
 * is expected to surface each step, not just the final answer). No echoed request fields,
 * consistent with every other calc-api response DTO in this codebase.
 */
public record SmokeProductionResponse(
		Double designHeatReleaseRate,
		Double convectiveHeatReleaseRate,
		Double flameHeight,
		Double heightAboveFire,
		PlumeRegimeDto plumeRegime,
		Double smokeTemperature,
		Double smokeDensity,
		Double volumetricFlowRate) {

}
