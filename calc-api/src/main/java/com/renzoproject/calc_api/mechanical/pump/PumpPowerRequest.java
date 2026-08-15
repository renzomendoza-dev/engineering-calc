package com.renzoproject.calc_api.mechanical.pump;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Standalone — doesn't require having called {@link PumpTDHController} first, though a frontend
 * would typically chain them.
 *
 * <p>{@code pumpEfficiency}'s upper bound ({@code <= 1}) is deliberately not enforced here
 * beyond {@code @Positive} — calc-core's {@code PumpPowerInput} already throws a clear
 * {@code CalculationException} for a value {@code > 1}, same "don't duplicate calc-core's own
 * validation" reasoning used throughout this DTO family.
 */
public record PumpPowerRequest(
		@NotNull @Positive Double flowRateValue,
		@NotNull String flowRateUnit,
		@NotNull @Positive Double totalDynamicHeadMeters,
		@NotNull @Positive Double pumpEfficiency,
		@NotNull @Positive Double fluidDensityKgM3) {

}
