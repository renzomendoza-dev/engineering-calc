package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;

import javax.measure.Quantity;

/**
 * Input for {@link FireWaterStorageCalculator}. {@code ratedPumpFlow} is a plain value -- this
 * calculator has no code coupling to {@code FirePumpCapacityResolver} or any fire pump suite
 * calculator, even though the value typically originates from one.
 *
 * @param ratedPumpFlow           must be positive
 * @param hazardClassification    required
 * @param selectedDurationMinutes optional; if {@code null}, {@link FireWaterStorageCalculator}
 *                                defaults to the resolved range's maximum (conservative). If
 *                                provided, must fall within the resolved
 *                                {@code [min, max]} range -- validated in the calculator, not
 *                                here, since the valid range depends on
 *                                {@link FireWaterDurationResolver}'s output
 * @param safetyMarginPercent     must not be negative; applied as
 *                                {@code (1 + safetyMarginPercent/100)} to the final volume
 * @throws CalculationException if {@code ratedPumpFlow}/{@code hazardClassification}/
 *                                {@code safetyMarginPercent} violate the rules above
 */
public record FireWaterStorageInput(
		Quantity<VolumetricFlowRate> ratedPumpFlow,
		HazardClassification hazardClassification,
		Double selectedDurationMinutes,
		double safetyMarginPercent) {

	public FireWaterStorageInput {
		if (ratedPumpFlow == null) {
			throw new CalculationException("ratedPumpFlow is required");
		}
		if (ratedPumpFlow.getValue().doubleValue() <= 0) {
			throw new CalculationException("ratedPumpFlow must be positive");
		}
		if (hazardClassification == null) {
			throw new CalculationException("hazardClassification is required");
		}
		if (safetyMarginPercent < 0) {
			throw new CalculationException("safetyMarginPercent must not be negative");
		}
	}

}
