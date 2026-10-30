package com.renzoproject.calc_api.smokecontrol.naturalventing;

import java.util.List;

/**
 * Checks for one zone that do not depend on a fire, mirroring calc-core's
 * {@code NaturalVentingResult.ZoneResult}.
 */
public record ZoneResultDto(String name, List<VentingCheckDto> checks) {

}
