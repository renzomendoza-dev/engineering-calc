package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * Where the system's water comes from. Each variant carries what that kind of source is actually
 * known by, rather than forcing every source into a flow test it may never have had:
 *
 * <ul>
 *   <li>{@link SupplyCurve} -- a city main or any source with a flow test: static pressure, and a
 *       residual pressure at a known flow.</li>
 *   <li>{@link GravityTankSupply} -- an elevated tank, known by the height of its water above the
 *       connection and the pipe run down to it.</li>
 * </ul>
 *
 * <p>A tank at or below the riser is deliberately absent: it has no pressure to offer there, so it
 * is not a supply but a pump's source. Such a system is calculated with <b>no</b> supply at all
 * ({@code waterSupply == null} on {@link SprinklerHydraulicInput}), which yields the demand figures
 * and leaves the margin and verdict unanswered -- the honest result when there is nothing to
 * compare against.
 *
 * <p>Sealed, and evaluated by {@link SprinklerHydraulicCalculator} rather than by the variants
 * themselves: a gravity tank's pressure at flow depends on reference data (pipe bores, fitting
 * equivalent lengths) that only the calculator holds resolvers for.
 */
public sealed interface WaterSupply permits SupplyCurve, GravityTankSupply {
}
