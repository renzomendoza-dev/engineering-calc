package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Evaluates power at both the rated point and the interpolated 150%-of-rated-flow point,
 * checking both against the driver — the fuller, safer alternative to {@link RatedPointOnly}.
 */
public record FullCurve(CandidatePumpCurve curve, double pumpEfficiency, double specificGravity, DriverType driverType)
		implements FirePumpPowerInput {

	public FullCurve {
		if (curve == null) {
			throw new CalculationException("curve is required");
		}
		if (pumpEfficiency <= 0 || pumpEfficiency > 1) {
			throw new CalculationException("pumpEfficiency must be in (0, 1]");
		}
		if (specificGravity <= 0) {
			throw new CalculationException("specificGravity must be positive");
		}
		if (driverType == null) {
			throw new CalculationException("driverType is required");
		}
	}

}
