package com.renzoproject.calc_api.mechanical.pipe;

/**
 * HTTP response body for a pipe velocity/sizing calculation.
 *
 * <p>Length values are always returned in millimetres and velocity values always in
 * metres/second — fixed canonical output units, not an echo of whatever unit the request used —
 * so the response shape is predictable regardless of input units.
 *
 * <p>Field population depends on {@code mode}:
 * <ul>
 *   <li>{@code VELOCITY_FROM_DIAMETER}: only {@code velocityValue}/{@code velocityUnit} are
 *       populated; every {@code DIAMETER_FROM_VELOCITY}-only field below is {@code null}.</li>
 *   <li>{@code DIAMETER_FROM_VELOCITY}: {@code calculatedMinDiameterValue}/{@code Unit},
 *       {@code nominalPipeSize}, {@code actualInternalDiameterValue}/{@code Unit}, and
 *       {@code actualVelocityValue}/{@code Unit} are populated; {@code velocityValue}/
 *       {@code velocityUnit} are {@code null}.</li>
 * </ul>
 */
public record PipeVelocityResponse(
		PipeSizingModeDto mode,

		Double velocityValue,
		String velocityUnit,

		Double calculatedMinDiameterValue,
		String calculatedMinDiameterUnit,
		String nominalPipeSize,
		Double actualInternalDiameterValue,
		String actualInternalDiameterUnit,
		Double actualVelocityValue,
		String actualVelocityUnit) {

}
