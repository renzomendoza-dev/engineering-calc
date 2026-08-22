package com.renzoproject.calc_api.mechanical.storage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a fire water storage calculation. GPM/gallons/minutes throughout,
 * matching calc-core's {@code FireWaterStorageCalculator} convention -- deliberately NOT SI,
 * unlike {@link DomesticWaterStorageRequest}.
 *
 * <p>Bean Validation covers structural bounds only -- it does NOT duplicate calc-core's domain
 * validation ({@code selectedDurationMinutes} must fall within the resolved hazard
 * classification's range). That stays exclusively in {@code FireWaterStorageCalculator} and
 * surfaces as a 400 via the existing {@code CalculationException} -> {@code GlobalExceptionHandler}
 * path -- that exception's message states the valid range, not just "invalid input".
 * {@code selectedDurationMinutes} is nullable: {@code null} means "use the conservative max of
 * the resolved range", same default-deferral pattern as the smoke-control requests'
 * resolver-backed optional fields.
 */
public record FireWaterStorageRequest(
		@NotNull @Positive Double ratedPumpFlowGpm,
		@NotNull HazardClassificationDto hazardClassification,
		Double selectedDurationMinutes,
		Double safetyMarginPercent) {

}
