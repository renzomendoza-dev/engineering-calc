package com.renzoproject.calc.core.smokecontrol;

/**
 * Plume regime when the height above the fire exceeds the flame height ({@code z > z_l}):
 * {@code m = 0.071 * Qc^(1/3) * z^(5/3) + 0.0018 * Qc}. Deliberate duplicate of {@link FarField}
 * -- see {@link TSquaredPlumeRegime}.
 */
public record TSquaredFarField(double massFlowRateKgS) implements TSquaredPlumeRegime {

}
