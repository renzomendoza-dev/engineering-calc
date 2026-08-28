package com.renzoproject.calc_api.electrical.powerconsumption;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a power consumption calculation.
 *
 * <p>{@code mode} / {@code circuitType} are plain strings mapped explicitly in
 * {@link PowerConsumptionMapper} — same rationale already documented on
 * {@code WireSizingRequest} / {@code WireSizingMapper}: a bad value should surface as a clear
 * "Unknown X: value" {@code CalculationException} → 400 via the existing
 * {@code GlobalExceptionHandler}, not a generic Jackson deserialization failure.
 *
 * <p>Only {@code totalOperatingHours} is validated here beyond type/blank checks — it's the one
 * field required regardless of {@code mode}. Every other field's cross-field requirement (which
 * ones must be null/non-null/positive for a given mode, including {@code pricePerKwh}'s
 * "positive if present" rule) is left entirely to calc-core's {@code PowerConsumptionInput}
 * compact constructor, which throws {@code CalculationException} on violation — deliberately not
 * duplicated here, matching the precedent already set by {@code ExpansionTankRequest}'s
 * unannotated, nullable {@code additionalVolumeFactor}.
 */
public record PowerConsumptionRequest(
		@NotBlank String mode,
		Double wattageInput,
		String circuitType,
		Double voltage,
		Double currentAmps,
		Double powerFactor,
		Double horsepowerInput,
		Double motorEfficiencyPercent,
		@Positive double totalOperatingHours,
		Double pricePerKwh) {

}
