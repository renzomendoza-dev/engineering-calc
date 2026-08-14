package com.renzoproject.calc_api.electrical.wiresizing;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Optional voltage drop cross-check nested in {@link WireSizingRequest}.
 *
 * <p>{@code circuitType} / {@code conduitMaterial} are plain strings mapped explicitly in
 * {@link WireSizingMapper} — see that class's Javadoc for why.
 */
public record VoltageDropCheckRequestDto(
		@NotBlank String circuitType,
		@Positive double oneWayLengthMeters,
		@DecimalMin(value = "0.0", inclusive = true) @DecimalMax(value = "1.0", inclusive = true) double powerFactor,
		@Positive double systemVoltage,
		@NotBlank String conduitMaterial,
		@Min(1) int parallelSetsPerPhase) {

}
