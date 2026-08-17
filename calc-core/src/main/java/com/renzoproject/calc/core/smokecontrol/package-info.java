/**
 * Smoke control calculators (NFPA 92-aligned): the axisymmetric plume smoke production
 * calculation, in two deliberately separate forms -- {@code SmokeProductionCalculator} (fixed
 * area/HRR-density design fire) and {@code TSquaredSmokeProductionCalculator} (t-squared growth
 * capped at a peak HRR, evaluated at one time point). Steps 2-8 of the plume correlation are
 * identical NFPA 92 math between the two, but they are intentionally NOT shared/refactored into
 * one calculator or a common base -- each has its own input shape, sealed plume-regime hierarchy,
 * and result record, so a change to one growth model never risks the other.
 *
 * <p>{@code common.AirPropertiesResolver} and this package's {@code SmokeControlDefaultsResolver}
 * ARE shared between both calculators -- that reference data is genuinely calculator-agnostic,
 * unlike the duplicated calculation logic above.
 *
 * <p><b>Units: this package works in plain SI-magnitude doubles throughout (m, m2, kW, kW/m2,
 * degC, kg/s, kg/m3, m3/s) rather than Indriya {@code Quantity} types.</b> This mirrors
 * {@code mechanical.firepump}'s deliberate choice to use its own self-consistent unit system
 * (psi/GPM there, plain-SI-double here) instead of the Indriya-quantity boundary convention used
 * in {@code mechanical.pipe}. The reason here is different but the precedent is the same: the
 * NFPA 92 plume correlation is a chain of empirical power-law formulas (flame height, mass flow,
 * smoke temperature, density) whose intermediate results — flame height, height above the fire,
 * mass flow rate — are consumed by the very next formula as plain numbers, not composed as
 * physical quantities a caller would reasonably want unit-converted mid-calculation; wrapping and
 * unwrapping each one in Indriya would add ceremony without benefit. This matches
 * {@code mechanical.pump}'s {@code PumpPowerResult} (plain {@code double} kW fields) more closely
 * than {@code mechanical.pipe}'s Indriya-typed outputs.
 */
package com.renzoproject.calc.core.smokecontrol;
