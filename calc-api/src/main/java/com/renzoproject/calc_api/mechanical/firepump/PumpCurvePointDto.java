package com.renzoproject.calc_api.mechanical.firepump;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Shared between {@link FirePumpCurveValidationRequest} and {@link FirePumpPowerRequest}
 * (its {@code FULL_CURVE} mode) — one pump curve point shape, not duplicated per endpoint.
 */
public record PumpCurvePointDto(
		@NotNull @PositiveOrZero Double flowGpm,
		@NotNull @Positive Double pressurePsi) {

}
