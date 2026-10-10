package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Sprinkler discharge relationship {@code Q = K * sqrt(P)} and its inverse.
 *
 * <p><b>Units:</b> {@code Q} in US gpm, {@code P} in psi, so {@code K} is in gpm/psi^0.5 (the
 * US nominal K-factor, not the metric K). Plain doubles, same reason as {@link HazenWilliams}.
 */
public final class SprinklerMath {

	private SprinklerMath() {
	}

	/**
	 * @param kFactor     sprinkler K, gpm/psi^0.5; must be positive
	 * @param pressurePsi discharge pressure at the sprinkler; must not be negative
	 * @throws CalculationException if either argument is out of range
	 */
	public static double flowGpm(double kFactor, double pressurePsi) {
		if (kFactor <= 0) {
			throw new CalculationException("kFactor must be positive (was " + kFactor + ")");
		}
		if (pressurePsi < 0) {
			throw new CalculationException("pressurePsi must not be negative (was " + pressurePsi + ")");
		}
		return kFactor * Math.sqrt(pressurePsi);
	}

	/**
	 * The pressure a sprinkler needs in order to discharge {@code flowGpm}: {@code P = (Q/K)^2}.
	 *
	 * @param kFactor sprinkler K, gpm/psi^0.5; must be positive
	 * @param flowGpm required discharge; must not be negative
	 * @throws CalculationException if either argument is out of range
	 */
	public static double pressurePsi(double kFactor, double flowGpm) {
		if (kFactor <= 0) {
			throw new CalculationException("kFactor must be positive (was " + kFactor + ")");
		}
		if (flowGpm < 0) {
			throw new CalculationException("flowGpm must not be negative (was " + flowGpm + ")");
		}
		double ratio = flowGpm / kFactor;
		return ratio * ratio;
	}

}
