package com.renzoproject.calc_api.smokecontrol;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a smoke production (plume) calculation. Mirrors calc-core's
 * {@code SmokeProductionInput} fields exactly.
 *
 * <p>Bean Validation here covers structural bounds only (required-ness, positivity) — it does
 * NOT duplicate calc-core's domain validation (e.g. {@code fireBaseHeight < ceilingHeight}, the
 * ambient-temperature reasonable-range check). Those stay exclusively in
 * {@code SmokeProductionInput}'s compact constructor and surface as a 400 via the existing
 * {@code CalculationException} → {@code GlobalExceptionHandler} path, same layering convention as
 * {@code DistanceAttenuationRequest}.
 *
 * <p>{@code convectiveFraction} and {@code fractionConvectiveHeatInSmokeLayer} are nullable with
 * no bounds annotation: when omitted, {@link SmokeProductionService} lets calc-core apply its own
 * {@code SmokeControlDefaultsResolver}-sourced default rather than this layer guessing or
 * enforcing a range calc-core itself doesn't enforce. {@code fireBaseHeight} is nullable for the
 * same reason — calc-core defaults it to {@code 0}. {@code ambientTemperature} has no
 * {@code @Positive}/{@code @PositiveOrZero} — negative and zero Celsius ambient temperatures are
 * physically valid.
 */
public record SmokeProductionRequest(
		@NotNull @Positive Double designFireArea,
		@NotNull @Positive Double heatReleaseRateDensity,
		Double convectiveFraction,
		@NotNull @Positive Double ceilingHeight,
		Double fireBaseHeight,
		@NotNull Double ambientTemperature,
		Double fractionConvectiveHeatInSmokeLayer) {

}
