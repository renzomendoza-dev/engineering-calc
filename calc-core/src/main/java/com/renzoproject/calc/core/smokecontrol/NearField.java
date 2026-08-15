package com.renzoproject.calc.core.smokecontrol;

/**
 * Plume regime when the height above the fire is at or below the flame height
 * ({@code z <= z_l}) -- the flame itself reaches into (or above) the ceiling, so a simpler
 * near-field entrainment correlation applies: {@code m = 0.032 * Qc^(3/5) * z}.
 */
public record NearField(double massFlowRateKgS) implements PlumeRegime {

}
