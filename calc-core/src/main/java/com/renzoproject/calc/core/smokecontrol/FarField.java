package com.renzoproject.calc.core.smokecontrol;

/**
 * Plume regime when the height above the fire exceeds the flame height ({@code z > z_l}) --
 * the flame doesn't reach the ceiling, so entrainment happens over the full height above the
 * fire: {@code m = 0.071 * Qc^(1/3) * z^(5/3) + 0.0018 * Qc}.
 */
public record FarField(double massFlowRateKgS) implements PlumeRegime {

}
