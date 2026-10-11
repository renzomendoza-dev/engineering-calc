package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;

/**
 * An elevated tank feeding the system by gravity.
 *
 * <p>Its pressure comes from height, not from a flow test: {@code 0.433 psi} per foot of water
 * above the connection at no flow, less the friction in the pipe run down to the connection as
 * flow increases. The calculator evaluates
 * {@code P(Q) = 0.433 * waterLevelHeightFt - friction(Q)} over {@code outletRuns}, which is a real
 * curve rather than a two-point fit -- and the reason the runs are asked for at all.
 *
 * <p>With no runs given the tank is treated as a constant-pressure source. That is the optimistic
 * reading (it credits the full head at every flow), so it is allowed but never assumed: a caller
 * that knows the piping should pass it.
 *
 * @param waterLevelHeightFt height of the tank's water level above the system's connection point,
 *                           ft; must be positive. A tank at or below the connection is not a
 *                           gravity supply at all -- see {@link WaterSupply}
 * @param outletRuns         pipe runs from the tank to the connection; may be empty or null
 * @throws CalculationException if the height is not positive
 */
public record GravityTankSupply(double waterLevelHeightFt, List<SupplyPipeRun> outletRuns) implements WaterSupply {

	public GravityTankSupply {
		if (waterLevelHeightFt <= 0) {
			throw new CalculationException("waterLevelHeightFt must be positive (was " + waterLevelHeightFt
					+ ") -- a tank at or below the connection has no pressure to offer there, so it is a pump's "
					+ "source rather than a supply; calculate it with no water supply instead");
		}
		outletRuns = outletRuns == null ? List.of() : List.copyOf(outletRuns);
	}

	/** Pressure at the connection with nothing flowing: the full head, before any friction. */
	public double staticPressurePsi() {
		return ElevationPressure.psiForRise(waterLevelHeightFt);
	}

}
