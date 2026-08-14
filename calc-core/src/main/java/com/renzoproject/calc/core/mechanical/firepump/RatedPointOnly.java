package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;
import javax.measure.quantity.Pressure;

/**
 * Evaluates power at only the rated point. Simpler than {@link FullCurve} when a full pump
 * curve isn't available, but this means the 150%-of-rated overload point is never checked — the
 * driver could come out undersized if the curve's overload BHP would actually exceed the rated
 * point's BHP (common, since BHP often rises with flow even as pressure falls).
 * {@link FirePumpPowerCalculator} surfaces this as a caveat in
 * {@code FirePumpPowerResult.driverSizingNote}, not silently.
 */
public record RatedPointOnly(
		Quantity<VolumetricFlowRate> flow,
		Quantity<Pressure> pressure,
		double pumpEfficiency,
		double specificGravity,
		DriverType driverType) implements FirePumpPowerInput {

	public RatedPointOnly {
		if (flow == null) {
			throw new CalculationException("flow is required");
		}
		if (flow.getValue().doubleValue() <= 0) {
			throw new CalculationException("flow must be positive");
		}
		if (pressure == null) {
			throw new CalculationException("pressure is required");
		}
		if (pressure.getValue().doubleValue() <= 0) {
			throw new CalculationException("pressure must be positive");
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
