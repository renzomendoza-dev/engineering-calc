package com.renzoproject.calc_api.electrical.wiresizing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a wire sizing calculation.
 *
 * <p>{@code insulationType} / {@code conductorMaterial} are plain strings mapped explicitly
 * in {@link WireSizingMapper} — see that class's Javadoc for why.
 *
 * <p>{@code terminationTempRatingCelsius} is only sanity-bounded here ({@code >= 60}); the
 * exact 60/75/90 match is left to {@code WireSizingInput}'s own validation in calc-core,
 * which throws {@code CalculationException} on violation, avoiding a duplicate exact-match
 * rule here.
 *
 * <p>{@code voltageDropCheck} is optional — {@code @Valid} cascades validation into it only
 * when present; a {@code null} value is not itself rejected.
 */
public record WireSizingRequest(
		@Positive double loadCurrentAmps,
		boolean isContinuousLoad,
		double ambientTempCelsius,
		@Min(1) int numberOfCurrentCarryingConductors,
		@NotBlank String insulationType,
		@NotBlank String conductorMaterial,
		@Min(60) int terminationTempRatingCelsius,
		@Valid VoltageDropCheckRequestDto voltageDropCheck) {

}
