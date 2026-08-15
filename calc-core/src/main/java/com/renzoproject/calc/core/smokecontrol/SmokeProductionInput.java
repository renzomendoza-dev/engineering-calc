package com.renzoproject.calc.core.smokecontrol;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Input for {@link SmokeProductionCalculator}. Direct values only -- no reference-table lookups
 * (unlike {@code ConductorPropertiesResolver}'s callers), matching the source design document's
 * calculation sheet, which takes these as raw project-specific inputs.
 *
 * @param designFireArea                     A, m2; must be positive
 * @param heatReleaseRateDensity             HRR, kW/m2; must be positive
 * @param convectiveFraction                 chi, dimensionless; nullable -- when omitted,
 *                                            {@link SmokeProductionCalculator} falls back to
 *                                            {@link SmokeControlDefaultsResolver#defaults()}
 * @param ceilingHeight                      m; must be positive
 * @param fireBaseHeight                     m; nullable -- defaults to {@code 0} when omitted
 *                                            (this default is fixed, not resolver-sourced, so
 *                                            it's applied here rather than in the calculator)
 * @param ambientTemperature                 To, degC; must fall within a physically reasonable
 *                                            range
 * @param fractionConvectiveHeatInSmokeLayer Ks, dimensionless; nullable -- same fallback as
 *                                            {@code convectiveFraction}
 * @throws CalculationException if any of the above rules is violated
 */
public record SmokeProductionInput(
		double designFireArea,
		double heatReleaseRateDensity,
		Double convectiveFraction,
		double ceilingHeight,
		Double fireBaseHeight,
		double ambientTemperature,
		Double fractionConvectiveHeatInSmokeLayer) {

	private static final double MIN_AMBIENT_TEMPERATURE_C = -40.0;
	private static final double MAX_AMBIENT_TEMPERATURE_C = 60.0;

	public SmokeProductionInput {
		if (designFireArea <= 0) {
			throw new CalculationException("designFireArea must be positive");
		}
		if (heatReleaseRateDensity <= 0) {
			throw new CalculationException("heatReleaseRateDensity must be positive");
		}
		if (ceilingHeight <= 0) {
			throw new CalculationException("ceilingHeight must be positive");
		}
		if (ambientTemperature < MIN_AMBIENT_TEMPERATURE_C || ambientTemperature > MAX_AMBIENT_TEMPERATURE_C) {
			throw new CalculationException("ambientTemperature " + ambientTemperature + " degC is outside the "
					+ "physically reasonable range (" + MIN_AMBIENT_TEMPERATURE_C + " to " + MAX_AMBIENT_TEMPERATURE_C
					+ " degC)");
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
