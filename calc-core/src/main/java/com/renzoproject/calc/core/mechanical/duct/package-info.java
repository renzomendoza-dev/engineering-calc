/**
 * Duct sizing calculators (ASHRAE Fundamentals Ch.21-aligned): {@code DuctSizingCalculator}
 * supports both Equal Friction and Velocity sizing methods, round and rectangular shapes, with
 * temperature/altitude-corrected air properties.
 *
 * <p>Shares {@code mechanical.FrictionFactorCalculator} and {@code mechanical.BisectionSolver}
 * with {@code mechanical.pipe} rather than duplicating either -- unlike {@code smokecontrol}'s
 * two plume calculators (which deliberately duplicate their shared math), the friction factor
 * correlation and the generic root-finder here are genuinely identical, reusable logic with no
 * domain-specific branching, so sharing is the right call rather than the "duplicate, don't
 * share" precedent set elsewhere in this codebase.
 *
 * <p><b>Units: ASHRAE's published duct equations (Fundamentals Ch.21, Equations 18-21) use
 * hydraulic diameter in millimeters with unit-conversion constants baked directly into the
 * formulas</b> (e.g. a 1000x factor in the Darcy equation, a 66.4 constant in the simplified
 * Reynolds number formula for standard air). Those mm-based formulas are NOT ported verbatim
 * here. Instead, {@code DuctSizingCalculator} converts its own inputs to the same meter-based SI
 * convention {@code PipePressureLossCalculator} already uses internally, calls the shared
 * meter-based utilities, and converts back only at its own input/output boundary. The one
 * exception is ASHRAE's equivalent-diameter formula ({@code De = 1.30*(ab)^0.625/(a+b)^0.25}),
 * which -- unlike the friction/Reynolds formulas -- is dimensionally self-consistent (its
 * exponents already sum to a pure length-to-length ratio) and works correctly in meters directly
 * without a mm round-trip; see {@code DuctSizingCalculator}'s Javadoc on that method for the
 * dimensional-analysis check that confirms this.
 */
package com.renzoproject.calc.core.mechanical.duct;
