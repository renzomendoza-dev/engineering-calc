package com.renzoproject.calc_api.mechanical.pipe;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a pipe pressure loss calculation. SI units throughout (degrees C,
 * m3/s-family flow rate units, meters) — matches calc-core's convention for this package, a
 * deliberate contrast with the fire pump package's fixed psi/GPM fields. Unlike the fire pump
 * DTOs, {@code flowRateUnit} is a unit-string field here, same as {@code PipeVelocityRequest} —
 * SI convention means fixed *result* units, not fixed *input* units.
 *
 * <p>{@code diameterSpecType}/{@code nominalMaterial}/{@code nominalSchedule}/
 * {@code nominalLabel}/{@code rawDiameterValue}/{@code rawDiameterUnit} are the exact same
 * field names/shape {@code PipeVelocityRequest} already uses for its {@code DiameterSpec}
 * discriminator, so a frontend form component built for one works for the other. Unlike
 * {@code PipeVelocityRequest}, {@code diameterSpecType} is unconditionally required here — this
 * endpoint has no second mode that skips it.
 *
 * <p>{@link #isDiameterSpecFieldsValid()} enforces {@code diameterSpecType}-dependent required
 * fields, same {@code @AssertTrue} pattern as {@code PipeVelocityRequest.isModeFieldsValid()}.
 * {@code nominalSchedule} is deliberately not enforced here, same reasoning as
 * {@code PipeVelocityRequest} — see that class's Javadoc.
 */
public record PipePressureLossRequest(
		@NotNull String fluidKey,
		@NotNull Double fluidTemperatureCelsius,
		@NotNull @Positive Double flowRateValue,
		@NotNull String flowRateUnit,

		// --- diameter spec — same discriminated shape as PipeVelocityRequest ---
		@NotNull DiameterSpecTypeDto diameterSpecType,
		String nominalMaterial,
		String nominalSchedule,
		String nominalLabel,
		Double rawDiameterValue,
		String rawDiameterUnit,

		@NotNull @Positive Double pipeLengthMeters,
		@NotNull FrictionFactorMethodDto method) {

	@AssertTrue(message = "For NOMINAL diameterSpecType: nominalMaterial and nominalLabel are "
			+ "required. For RAW: rawDiameterValue and rawDiameterUnit are required.")
	public boolean isDiameterSpecFieldsValid() {
		if (diameterSpecType == null) {
			return true; // let @NotNull on diameterSpecType itself report this
		}
		return switch (diameterSpecType) {
			case NOMINAL -> notBlank(nominalMaterial) && notBlank(nominalLabel);
			case RAW -> rawDiameterValue != null && notBlank(rawDiameterUnit);
		};
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}

}
