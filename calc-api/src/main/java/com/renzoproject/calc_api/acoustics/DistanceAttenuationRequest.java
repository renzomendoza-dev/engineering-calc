package com.renzoproject.calc_api.acoustics;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a distance attenuation calculation. Mirrors calc-core's
 * {@code DistanceAttenuationInput} fields exactly.
 *
 * <p>Bean Validation here is layered on top of, not instead of, calc-core's own manual checks
 * in {@code DistanceAttenuationInput}'s compact constructor — matching the established
 * convention elsewhere in this codebase (e.g. {@code FirePumpCapacityRequest}) of validating at
 * both the DTO boundary (fast, Spring-native 400 with field-level detail) and the calc-core
 * boundary (the authoritative check, since calc-core has no Spring context of its own to trigger
 * Bean Validation and must be safe to call from any caller, not just this REST layer).
 *
 * <p>{@code referenceSplDb} has no {@code @Positive} — SPL can legitimately be very low or even
 * negative dB for quiet sounds relative to the 20 uPa reference threshold, same reasoning
 * calc-core's own input record already applies (it doesn't validate this field's sign either).
 */
public record DistanceAttenuationRequest(
		@NotNull Double referenceSplDb,
		@NotNull @Positive Double referenceDistance,
		@NotNull @Positive Double targetDistance) {

}
