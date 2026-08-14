package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;

/**
 * Rounds a computed demand flow up to the nearest NFPA 20 listed standard fire pump capacity.
 * Standalone — not wired into {@link FirePumpDemandCalculator} automatically, since not every
 * caller needs the rounded-up standard size immediately after computing demand.
 */
public interface FirePumpCapacityResolver {

	/**
	 * Selects the smallest listed standard capacity {@code >= ratedFlow}.
	 *
	 * @throws CalculationException if {@code ratedFlow} exceeds every listed capacity (never
	 *                              silently returns the largest one instead)
	 */
	StandardPumpRating resolveNextStandardCapacity(Quantity<VolumetricFlowRate> ratedFlow);

}
