package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc_api.electrical.wiresizing.VoltageDropCheckRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for the motor-to-wire-sizing pipeline: the motor-identification fields
 * {@link MotorFlcRequest} needs, plus the wire sizing condition fields {@code WireSizingRequest}
 * needs — minus {@code loadCurrentAmps} and {@code isContinuousLoad}, since both are derived
 * from the FLC calculation rather than supplied directly (see calc-core's
 * {@code MotorConductorSizingCalculator}).
 *
 * <p>Reuses {@link VoltageDropCheckRequestDto} directly from the wiresizing package rather than
 * redefining an equivalent nested type here.
 *
 * <p>See {@link MotorFlcRequest}'s Javadoc for why {@code phaseType} / {@code motorClass} are
 * plain strings and why {@code motorClass} has no cross-field annotation.
 */
public record MotorConductorSizingRequest(
		@NotBlank String phaseType,
		String motorClass,
		@NotBlank String horsepowerLabel,
		@Positive int voltage,
		@Min(1) @Max(100) Integer synchronousPowerFactorPercent,
		double ambientTempCelsius,
		@Min(1) int numberOfCurrentCarryingConductors,
		@NotBlank String insulationType,
		@NotBlank String conductorMaterial,
		@Min(60) int terminationTempRatingCelsius,
		@Valid VoltageDropCheckRequestDto voltageDropCheck) {

}
