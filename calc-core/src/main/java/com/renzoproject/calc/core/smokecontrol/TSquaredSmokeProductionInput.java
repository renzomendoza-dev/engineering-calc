package com.renzoproject.calc.core.smokecontrol;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input for {@link TSquaredSmokeProductionCalculator}. Deliberately separate from
 * {@link SmokeProductionInput} -- a t-squared design fire is specified by a growth rate and a
 * capping HRR evaluated at one time point, not a fixed area/HRR-density pair, so the two input
 * shapes are genuinely different, not a refactor candidate.
 *
 * @param fireGrowthRate                     alpha, kW/s^2; must be positive
 * @param cappingHRR                         Qcap, kW; must be positive
 * @param evaluationTime                     t, seconds; must be {@code >= 0}
 * @param convectiveFraction                 chi, dimensionless; nullable -- when omitted,
 *                                            {@link TSquaredSmokeProductionCalculator} falls back
 *                                            to {@link SmokeControlDefaultsResolver#defaults()}
 * @param ceilingHeight                      m -- the height to evaluate smoke production at (may
 *                                            be a design interface height rather than the
 *                                            physical ceiling, same semantics as
 *                                            {@link SmokeProductionInput#ceilingHeight()}); must
 *                                            be positive
 * @param fireBaseHeight                     m; nullable -- defaults to {@code 0} when omitted
 * @param ambientTemperature                 To, degC
 * @param fractionConvectiveHeatInSmokeLayer Ks, dimensionless; nullable -- same fallback as
 *                                            {@code convectiveFraction}
 * @throws CalculationException if any of the above rules is violated
 */
public record TSquaredSmokeProductionInput(
		double fireGrowthRate,
		double cappingHRR,
		double evaluationTime,
		Double convectiveFraction,
		double ceilingHeight,
		Double fireBaseHeight,
		double ambientTemperature,
		Double fractionConvectiveHeatInSmokeLayer) {

	public TSquaredSmokeProductionInput {
		if (fireGrowthRate <= 0) {
			throw new CalculationException("fireGrowthRate must be positive");
		}
		if (cappingHRR <= 0) {
			throw new CalculationException("cappingHRR must be positive");
		}
		if (evaluationTime < 0) {
			throw new CalculationException("evaluationTime must not be negative");
		}
		if (ceilingHeight <= 0) {
			throw new CalculationException("ceilingHeight must be positive");
		}
		if (fireBaseHeight == null) {
			fireBaseHeight = 0.0;
		}
		if (fireBaseHeight >= ceilingHeight) {
			throw new CalculationException("fireBaseHeight must be less than ceilingHeight "
					+ "(height above the fire must be positive)");
		}
	}

}
