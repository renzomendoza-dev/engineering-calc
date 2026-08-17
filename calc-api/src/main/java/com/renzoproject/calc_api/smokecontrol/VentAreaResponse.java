package com.renzoproject.calc_api.smokecontrol;

/**
 * HTTP response body for a natural smoke vent area calculation. Mirrors calc-core's
 * {@code VentAreaResult} exactly -- both the intermediate {@code temperatureDifference} and the
 * final {@code requiredVentArea} are exposed, not just the area. No echoed request fields,
 * consistent with every other calc-api response DTO in this codebase.
 */
public record VentAreaResponse(Double temperatureDifference, Double requiredVentArea) {

}
