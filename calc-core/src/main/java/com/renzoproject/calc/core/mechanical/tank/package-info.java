/**
 * Bladder/diaphragm tank sizing calculators, both reducing to the same Boyle's Law shape (see
 * {@code mechanical.BoyleTankSizing}) but solving physically distinct problems, kept as separate
 * calculators with separate input types rather than merged into one:
 * <ul>
 *   <li>{@code ExpansionTankCalculator} (ASPE/ASHRAE method) -- thermal expansion tank sizing for
 *       a domestic water heater or hydronic heating system.</li>
 *   <li>{@code PressureTankCalculator} -- hydropneumatic pressure tank sizing for a well/booster
 *       pump system, for either conventional (on/off pressure switch) or VFD pump control.</li>
 * </ul>
 * Plain (non-bladder) compression tanks use a different formula/design factor and are out of
 * scope for both.
 *
 * <p>Both reuse {@code mechanical.StandardAtmosphere} for gauge-to-absolute pressure conversion
 * at altitude and {@code mechanical.BoyleTankSizing} for the shared
 * {@code requiredVolume = usableVolume / (1 - Plow/Phigh)} formula.
 * {@code ExpansionTankCalculator} additionally reuses {@code mechanical.pipe.FluidPropertiesResolver}
 * directly for cold/hot water density (the same {@code WATER} reference table
 * {@code PipePressureLossCalculator} uses). All three are genuinely shared, domain-agnostic logic,
 * so reusing them follows the same "share, don't duplicate" reasoning documented in
 * {@code mechanical.duct}'s package-info for {@code FrictionFactorCalculator}/
 * {@code BisectionSolver}.
 *
 * <p><b>Units: SI</b> (cubic metres for volume, Pascal for pressure, Celsius for temperature,
 * metres for length/altitude) -- consistent with the rest of {@code mechanical}, not the fire
 * suite's US-customary convention.
 */
package com.renzoproject.calc.core.mechanical.tank;
