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
 *
 * <p>{@code overcurrentDeviceAmps} is optional: the rating of the device that will protect the
 * conductor. Given, a size whose PEC 2.40.1.4(D) small-conductor limit is below it is passed over
 * for the next size up.
 *
 * <p>{@code nipple} is optional (a nullable {@code Boolean} so requests without it stay valid):
 * {@code true} for a nipple of not more than 600 mm, where PEC Chapter 10 note (4) lets the
 * conductor-count adjustment go unapplied.
 */
public record WireSizingRequest(
		@Positive double loadCurrentAmps,
		boolean isContinuousLoad,
		double ambientTempCelsius,
		@Min(1) int numberOfCurrentCarryingConductors,
		@Min(1) int numberOfParallelSets,
		@NotBlank String insulationType,
		@NotBlank String conductorMaterial,
		@Min(60) int terminationTempRatingCelsius,
		@Valid VoltageDropCheckRequestDto voltageDropCheck,
		@Positive Double overcurrentDeviceAmps,
		Boolean nipple) {

}
