package com.renzoproject.calc_api.smokecontrol;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * HTTP request body for a t-squared fire growth smoke production calculation. Mirrors calc-core's
 * {@code TSquaredSmokeProductionInput} fields exactly. Deliberately separate from
 * {@link SmokeProductionRequest} -- not sharing fields/logic, matching the "fully separate,
 * duplicated" decision already made at the core layer.
 *
 * <p>Bean Validation here covers structural bounds only -- it does NOT duplicate calc-core's
 * domain validation (e.g. {@code fireBaseHeight < ceilingHeight}). That stays exclusively in
 * {@code TSquaredSmokeProductionInput}'s compact constructor and surfaces as a 400 via the
 * existing {@code CalculationException} -> {@code GlobalExceptionHandler} path, same layering
 * convention as {@link SmokeProductionRequest}.
 *
 * <p>{@code convectiveFraction} and {@code fractionConvectiveHeatInSmokeLayer} are nullable with
 * no bounds annotation: when omitted, {@link TSquaredSmokeProductionService} lets calc-core apply
 * its own {@code SmokeControlDefaultsResolver}-sourced default. {@code fireBaseHeight} is
 * nullable for the same reason -- calc-core defaults it to {@code 0}. {@code ambientTemperature}
 * has no {@code @Positive}/{@code @PositiveOrZero} -- negative Celsius ambient temperatures are
 * physically valid. {@code evaluationTime} uses {@code @PositiveOrZero} (not {@code @Positive})
 * since {@code t = 0} is a valid evaluation point.
 */
public record TSquaredSmokeProductionRequest(
		@NotNull @Positive Double fireGrowthRate,
		@NotNull @Positive Double cappingHRR,
		@NotNull @PositiveOrZero Double evaluationTime,
		Double convectiveFraction,
		@NotNull @Positive Double ceilingHeight,
		Double fireBaseHeight,
		@NotNull Double ambientTemperature,
		Double fractionConvectiveHeatInSmokeLayer) {

}
