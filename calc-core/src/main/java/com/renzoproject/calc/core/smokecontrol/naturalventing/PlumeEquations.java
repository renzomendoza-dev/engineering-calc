package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingResult.Regime;

/**
 * The plume of NFPA 204 (2021) Section 9.2.3: mean flame height (9.2.3.1), virtual origin (9.2.3.2)
 * and mass flow at the layer boundary (9.2.3.6 below the flames, 9.2.3.7 where they reach it).
 * Shared by the design calculation, the image-fire screening and the time-stepping check, which all
 * apply the same equations at different heat release rates.
 */
final class PlumeEquations {

	private PlumeEquations() {
	}

	/**
	 * @param flameHeight   L, m
	 * @param virtualOrigin z0, m
	 * @param regime        which equation applied
	 * @param massFlow      mp, kg/s, before any line-fire factor
	 */
	record Plume(double flameHeight, double virtualOrigin, Regime regime, double massFlow) {
	}

	/**
	 * @param q        heat release rate, kW
	 * @param diameter equivalent fire diameter, m
	 * @param zs       height of the layer boundary above the base of the fire, m
	 * @param context  what the plume is for, in the message when it cannot be worked out
	 * @throws CalculationException if the layer boundary is at or below the virtual origin
	 */
	static Plume plume(double q, double diameter, double zs, String context) {
		double qc = NaturalVentingCalculator.CONVECTIVE_FRACTION * q;
		double flameHeight = -1.02 * diameter + 0.235 * Math.pow(q, 2.0 / 5.0);
		double virtualOrigin = 0.083 * Math.pow(q, 2.0 / 5.0) - 1.02 * diameter;
		if (flameHeight < zs) {
			double height = zs - virtualOrigin;
			if (height <= 0) {
				throw new CalculationException(context
						+ ": the smoke layer boundary is at or below the virtual origin of the plume, so Equation 9.2.3.6 does not apply");
			}
			double massFlow = 0.071 * Math.cbrt(qc) * Math.pow(height, 5.0 / 3.0)
					* (1 + 0.027 * Math.pow(qc, 2.0 / 3.0) * Math.pow(height, -5.0 / 3.0));
			return new Plume(flameHeight, virtualOrigin, Regime.BELOW_LAYER, massFlow);
		}
		return new Plume(flameHeight, virtualOrigin, Regime.REACHES_LAYER, 0.0056 * qc * zs / flameHeight);
	}

}
