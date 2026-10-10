package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * One flowing sprinkler inside the remote design area.
 *
 * @param nodeId           node in the pipe network this sprinkler discharges from
 * @param kFactor          sprinkler K, gpm/psi^0.5; must be positive
 * @param coverageAreaSqFt area this sprinkler protects, ft2; must be positive. Multiplied by the
 *                         design density to get the sprinkler's minimum required flow
 * @throws CalculationException if any rule above is violated
 */
public record RemoteAreaSprinkler(
		String nodeId,
		double kFactor,
		double coverageAreaSqFt) {

	public RemoteAreaSprinkler {
		if (nodeId == null || nodeId.isBlank()) {
			throw new CalculationException("sprinkler nodeId is required");
		}
		if (kFactor <= 0) {
			throw new CalculationException("kFactor must be positive for sprinkler " + nodeId);
		}
		if (coverageAreaSqFt <= 0) {
			throw new CalculationException("coverageAreaSqFt must be positive for sprinkler " + nodeId);
		}
	}

}
