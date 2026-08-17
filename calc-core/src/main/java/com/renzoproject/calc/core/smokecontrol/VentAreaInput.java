package com.renzoproject.calc.core.smokecontrol;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input for {@link VentAreaCalculator}. Deliberately decoupled from
 * {@link SmokeProductionCalculator}/{@link TSquaredSmokeProductionCalculator} -- this calculator
 * takes plain values (the smoke volumetric flow rate, temperatures, vent height), not a
 * reference to either upstream calculator's result, so it can be called from a caller who already
 * has these values from any source.
 *
 * @param volumetricFlowRate  V, m3/s -- the smoke volumetric flow rate to be vented; must be
 *                            positive
 * @param smokeTemperature    Ts, degC
 * @param ambientTemperature  To, degC
 * @param ventHeight          H, m -- height from the smoke layer to the vent; must be positive
 * @param dischargeCoefficient Cd, dimensionless; nullable -- when omitted,
 *                            {@link VentAreaCalculator} falls back to
 *                            {@link SmokeControlDefaultsResolver#defaults()}; if supplied, must
 *                            be in {@code (0, 1]}
 * @throws CalculationException if any of the above rules is violated, or if
 *                               {@code smokeTemperature} is not strictly greater than
 *                               {@code ambientTemperature} (a non-positive buoyancy driver makes
 *                               the vent area formula physically meaningless)
 */
public record VentAreaInput(
		double volumetricFlowRate,
		double smokeTemperature,
		double ambientTemperature,
		double ventHeight,
		Double dischargeCoefficient) {

	public VentAreaInput {
		if (volumetricFlowRate <= 0) {
			throw new CalculationException("volumetricFlowRate must be positive");
		}
		if (ventHeight <= 0) {
			throw new CalculationException("ventHeight must be positive");
		}
		if (smokeTemperature <= ambientTemperature) {
			throw new CalculationException("smokeTemperature must be strictly greater than ambientTemperature "
					+ "(deltaT = " + (smokeTemperature - ambientTemperature) + " degC is not positive -- the vent "
					+ "area formula requires a positive buoyancy driver)");
		}
		if (dischargeCoefficient != null && (dischargeCoefficient <= 0 || dischargeCoefficient > 1)) {
			throw new CalculationException("dischargeCoefficient must be in (0, 1]");
		}
	}

}
