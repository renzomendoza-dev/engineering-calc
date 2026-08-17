package com.renzoproject.calc_api.smokecontrol;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP request body for a natural smoke vent area calculation. Mirrors calc-core's
 * {@code VentAreaInput} fields exactly. Standalone -- does not reference, embed, or accept a
 * result from either smoke production endpoint; the caller supplies plain values directly, same
 * decoupling decision made at the core layer.
 *
 * <p>Bean Validation here covers structural bounds only -- it does NOT duplicate calc-core's
 * domain validation ({@code deltaT} must be strictly positive, {@code dischargeCoefficient} must
 * be in {@code (0, 1]} when supplied). Those stay exclusively in {@code VentAreaInput}'s compact
 * constructor and surface as a 400 via the existing {@code CalculationException} ->
 * {@code GlobalExceptionHandler} path, same layering convention as the other smoke-control
 * requests. {@code smokeTemperature}/{@code ambientTemperature} have no
 * {@code @Positive}/{@code @PositiveOrZero} -- negative Celsius is physically valid (if
 * unlikely), and this layer doesn't add an artificial floor calc-core itself doesn't enforce.
 *
 * <p>{@code dischargeCoefficient} is nullable with no bounds annotation: when omitted,
 * {@link VentAreaService} lets calc-core apply its own {@code SmokeControlDefaultsResolver}
 * -sourced default, same pattern as {@code convectiveFraction} in the plume request DTOs.
 */
public record VentAreaRequest(
		@NotNull @Positive Double volumetricFlowRate,
		@NotNull Double smokeTemperature,
		@NotNull Double ambientTemperature,
		@NotNull @Positive Double ventHeight,
		Double dischargeCoefficient) {

}
