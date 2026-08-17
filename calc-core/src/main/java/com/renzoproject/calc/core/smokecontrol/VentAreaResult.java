package com.renzoproject.calc.core.smokecontrol;

/**
 * Result of {@link VentAreaCalculator}. Exposes {@code deltaT} alongside the final
 * {@code requiredVentArea} rather than collapsing to just the area, consistent with how the
 * Smoke Production calculators expose every intermediate value.
 *
 * @param deltaT           Ts - To, degC -- the buoyancy driver
 * @param requiredVentArea Av, m2
 */
public record VentAreaResult(double deltaT, double requiredVentArea) {

}
