/**
 * Fire sprinkler hydraulic calculations (NFPA 13 2019): {@code SprinklerHydraulicCalculator} works
 * a remote design area from the most remote sprinkler back to the water supply and compares the
 * result against a supply flow test.
 *
 * <p><b>Friction loss is Hazen-Williams, not Darcy-Weisbach.</b> NFPA 13 mandates Hazen-Williams
 * for sprinkler piping, so {@link com.renzoproject.calc.core.mechanical.sprinkler.HazenWilliams}
 * exists alongside {@code mechanical.FrictionFactorCalculator} rather than one delegating to the
 * other. They are different correlations for different standards, not duplicated math:
 * Hazen-Williams carries an empirical C factor and a unit-specific coefficient, while
 * Darcy-Weisbach needs fluid density/viscosity and a computed friction factor. Mixing them would
 * make this package's output non-compliant and the pipe package's output wrong.
 *
 * <p><b>Units: psi, US gpm, feet, inches and fps throughout</b>, like {@code mechanical.firepump}
 * and for the same reason (see that package's Javadoc): NFPA 13's tables, the 4.52 Hazen-Williams
 * coefficient, the 0.433 psi/ft elevation constant, K-factors and published fitting equivalent
 * lengths are all expressed in those units, and converting to SI internally would mean re-deriving
 * every constant with no benefit while making it harder to check this code against a hand-worked
 * calculation sheet. psi, gpm and feet are reused from
 * {@code mechanical.firepump.FirePumpUnits}; inches and fps are added in
 * {@link com.renzoproject.calc.core.mechanical.sprinkler.SprinklerUnits}.
 *
 * <p><b>What is reused rather than rebuilt:</b> pipe inside diameters come from
 * {@code mechanical.pipe.PipeDimensionResolver} (so a nominal size is never used as a bore), and
 * per-segment velocity comes from {@code mechanical.pipe.PipeVelocityCalculator}.
 *
 * <p><b>What the caller decides, not this package:</b> design density, design area, hose stream
 * allowance, sprinkler K-factors and each segment's C factor are all inputs. The NFPA 13
 * density/area curve gives a range of valid design points, a listed sprinkler's K comes from its
 * data sheet, and a designer may choose a more conservative C than the table's. The resolvers here
 * ({@code HazardPresetResolver}, {@code CFactorResolver}, {@code SprinklerKFactorResolver}) publish
 * the reference sets for a caller or a form to default from; only the fitting equivalent lengths
 * and pipe dimensions are consumed by the calculator itself.
 *
 * <p><b>Scope:</b> tree networks only, single remote area, no gridded/looped solution, no
 * pipe-schedule (non-hydraulic) method, no tank/pump sizing -- those live in
 * {@code mechanical.storage} and {@code mechanical.firepump}.
 */
package com.renzoproject.calc.core.mechanical.sprinkler;
