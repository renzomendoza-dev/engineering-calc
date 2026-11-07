package com.renzoproject.calc_api.smokecontrol.naturalventing;

/**
 * The smoke layer of a fire against a wall or in a corner by the image-fire method (supplementary, not
 * NFPA 204), mirroring calc-core's {@code NaturalVentingResult.ImageFireScenario}. Units: kW, kg/s, K.
 */
public record ImageFireScenarioDto(
		double multiplier,
		double equivalentHeatReleaseRate,
		double plumeMassFlow,
		double layerTemperatureRise,
		double layerTemperature) {

}
