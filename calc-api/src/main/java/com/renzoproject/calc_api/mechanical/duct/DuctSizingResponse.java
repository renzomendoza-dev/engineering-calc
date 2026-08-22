package com.renzoproject.calc_api.mechanical.duct;

/**
 * HTTP response body for a duct sizing calculation. Mirrors calc-core's {@code DuctSizingResult}
 * exactly, in the same mixed HVAC-industry units as {@link DuctSizingRequest} (millimeters for
 * dimensions, m/s for velocity, Pa/m for friction rate) -- explicit {@code Mm}/{@code Mps}
 * suffixes since this differs from {@code PipePressureLossResponse}'s pure-SI (meters)
 * convention. {@code ductWidthMm}/{@code ductHeightMm} are {@code null} for {@code ROUND} shape.
 */
public record DuctSizingResponse(
		Double equivalentDiameterMm,
		Double ductWidthMm,
		Double ductHeightMm,
		Double actualVelocityMps,
		Double reynoldsNumber,
		Double frictionFactor,
		Double actualFrictionRatePaPerM) {

}
