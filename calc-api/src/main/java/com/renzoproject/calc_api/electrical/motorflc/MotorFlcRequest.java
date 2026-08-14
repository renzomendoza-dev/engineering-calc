package com.renzoproject.calc_api.electrical.motorflc;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a standalone motor full-load current lookup.
 *
 * <p>{@code phaseType} / {@code motorClass} are plain strings mapped explicitly in
 * {@link MotorFlcMapper} — same rationale as wire sizing's plain-enum fields. {@code motorClass}
 * is deliberately left without a cross-field {@code @NotNull}/{@code @Null} rule here: calc-core's
 * {@code MotorFlcInput} compact constructor already rejects an invalid phaseType/motorClass
 * combination with a clear message, so duplicating that rule in annotations would just be a
 * second place for the same validation to drift out of sync.
 *
 * <p>{@code horsepowerLabel} is passed through exactly as received — never parsed or
 * reformatted here — since calc-core's tables key on the label string verbatim (e.g.
 * {@code "1/4"}, {@code "1 1/2"}, {@code "10"}).
 *
 * <p>{@code synchronousPowerFactorPercent} is only sanity-bounded to a 1-100 range here; the
 * exact 100/90/80 match, and the "only valid when motorClass is SYNCHRONOUS" rule, are left to
 * calc-core's own validation.
 */
public record MotorFlcRequest(
		@NotBlank String phaseType,
		String motorClass,
		@NotBlank String horsepowerLabel,
		@Positive int voltage,
		@Min(1) @Max(100) Integer synchronousPowerFactorPercent) {

}
