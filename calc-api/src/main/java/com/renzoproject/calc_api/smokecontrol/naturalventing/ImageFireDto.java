package com.renzoproject.calc_api.smokecontrol.naturalventing;

/**
 * The layer for a fire against a wall and in a corner, mirroring calc-core's {@code NaturalVentingResult.ImageFire}.
 */
public record ImageFireDto(ImageFireScenarioDto wall, ImageFireScenarioDto corner) {

}
