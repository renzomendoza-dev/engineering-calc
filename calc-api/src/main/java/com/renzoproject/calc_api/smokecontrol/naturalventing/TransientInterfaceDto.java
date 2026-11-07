package com.renzoproject.calc_api.smokecontrol.naturalventing;

/**
 * The supplementary time-stepping check of the smoke layer boundary (not NFPA 204), mirroring calc-core's
 * {@code NaturalVentingResult.TransientInterface}. Units: s, m. The two times are null when the boundary does
 * not fall below the design height within {@code horizon}.
 */
public record TransientInterfaceDto(
		double endTime,
		double interfaceHeightAtEnd,
		Double timeToDesignHeight,
		Double timeToDesignHeightWithoutVents,
		double horizon) {

}
