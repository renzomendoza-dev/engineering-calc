package com.renzoproject.calc.core.smokecontrol;

/**
 * Which NFPA 92 axisymmetric plume correlation applied -- a sealed interface rather than a flat
 * record with an unused field, mirroring {@code mechanical.pipe}'s {@code DiameterSpec} pattern:
 * {@link FarField} and {@link NearField} compute mass flow via genuinely different formulas
 * (height above the fire vs. flame height governs which), so {@link SmokeProductionCalculator}
 * pattern-matches with a switch expression to build the right one.
 *
 * <p>Unlike {@code DiameterSpec}, both permitted records share one field ({@code massFlowRateKgS}
 * -- same computed quantity, different formula), so that shared accessor is declared here rather
 * than re-extracted per {@code switch} arm, same reasoning as {@code FireAlarmAudibilityInput}.
 */
public sealed interface PlumeRegime permits FarField, NearField {

	double massFlowRateKgS();

}
