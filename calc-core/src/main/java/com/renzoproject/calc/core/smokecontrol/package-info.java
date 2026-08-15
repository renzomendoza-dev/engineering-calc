/**
 * Smoke control calculators (NFPA 92-aligned), starting with the axisymmetric plume smoke
 * production calculation.
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
