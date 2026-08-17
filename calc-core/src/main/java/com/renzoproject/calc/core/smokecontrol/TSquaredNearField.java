package com.renzoproject.calc.core.smokecontrol;

/**
 * Plume regime when the height above the fire is at or below the flame height
 * ({@code z <= z_l}): {@code m = 0.032 * Qc^(3/5) * z}. Deliberate duplicate of
 * {@link NearField} -- see {@link TSquaredPlumeRegime}.
 */
public record TSquaredNearField(double massFlowRateKgS) implements TSquaredPlumeRegime {

}
