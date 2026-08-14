package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Rounds a computed brake horsepower up to the nearest standard NEMA electric motor HP step.
 * Electric drivers only — see {@code reference/firepump/README.md} on why diesel drivers don't
 * get a resolver here.
 */
public interface FirePumpMotorSizeResolver {

	/**
	 * @throws CalculationException if {@code brakeHorsepower} exceeds every listed step (never
	 *                              silently returns the largest one instead)
	 */
	double resolveNextStandardMotorHp(double brakeHorsepower);

}
