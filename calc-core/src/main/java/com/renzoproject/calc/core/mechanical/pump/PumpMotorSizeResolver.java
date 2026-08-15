package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Rounds a computed shaft power up to the nearest standard IEC electric motor kW step.
 * Electric drivers only — see {@code reference/pump/README.md} on why diesel drivers don't get
 * a resolver here.
 */
public interface PumpMotorSizeResolver {

	/**
	 * @throws CalculationException if {@code shaftPowerKw} exceeds every listed step (never
	 *                              silently returns the largest one instead)
	 */
	double resolveNextStandardMotorKw(double shaftPowerKw);

}
