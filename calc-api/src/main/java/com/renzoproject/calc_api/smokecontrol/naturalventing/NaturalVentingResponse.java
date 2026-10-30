package com.renzoproject.calc_api.smokecontrol.naturalventing;

import java.util.List;

/**
 * Result of an NFPA 204 natural venting calculation, mirroring calc-core's
 * {@code NaturalVentingResult}: one {@link FireCaseResultDto} per fire case and one
 * {@link ZoneResultDto} per zone, in request order.
 */
public record NaturalVentingResponse(
		List<FireCaseResultDto> fireCases,
		List<ZoneResultDto> zones) {

}
