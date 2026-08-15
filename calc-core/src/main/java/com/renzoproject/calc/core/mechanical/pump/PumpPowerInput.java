package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;
import javax.measure.quantity.Length;

/**
 * Input for {@link PumpPowerCalculator}. Standalone — not required to come from
 * {@link PumpTDHCalculator}, though {@code totalDynamicHead} typically will.
 *
 * @param fluidDensityKgM3 supplied directly rather than resolved via
 *                         {@code FluidPropertiesResolver} — keeps this calculator usable on its
 *                         own without a fluid-key/temperature/resolver dependency
 * @throws CalculationException if any validation rule below is violated. Unlike
 *                              {@link PumpTDHCalculator}, a non-positive {@code totalDynamicHead}
 *                              IS invalid here — you can't size a motor for a pump doing no or
 *                              negative work.
 */
public record PumpPowerInput(
		Quantity<VolumetricFlowRate> flowRate,
		Quantity<Length> totalDynamicHead,
		double pumpEfficiency,
		double fluidDensityKgM3) {

	public PumpPowerInput {
		if (flowRate == null) {
			throw new CalculationException("flowRate is required");
		}
		if (flowRate.getValue().doubleValue() <= 0) {
			throw new CalculationException("flowRate must be positive");
		}
		if (totalDynamicHead == null) {
			throw new CalculationException("totalDynamicHead is required");
		}
		if (totalDynamicHead.getValue().doubleValue() <= 0) {
			throw new CalculationException("totalDynamicHead must be positive");
		}
		if (pumpEfficiency <= 0 || pumpEfficiency > 1) {
			throw new CalculationException("pumpEfficiency must be in (0, 1]");
		}
		if (fluidDensityKgM3 <= 0) {
			throw new CalculationException("fluidDensityKgM3 must be positive");
		}
	}

}
