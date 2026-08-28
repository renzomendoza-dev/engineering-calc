package com.renzoproject.calc_api.electrical.powerconsumption;

import com.renzoproject.calc.core.electrical.powerconsumption.PowerConsumptionResult;

/**
 * HTTP response body for a power consumption calculation, mirroring calc-core's
 * {@link PowerConsumptionResult}.
 */
public record PowerConsumptionResponse(
		double powerWatts,
		double powerKw,
		double energyConsumedKwh,
		Double estimatedCost,
		boolean isEstimatedPower) {

	public static PowerConsumptionResponse from(PowerConsumptionResult result) {
		return new PowerConsumptionResponse(
				result.powerWatts(),
				result.powerKw(),
				result.energyConsumedKwh(),
				result.estimatedCost(),
				result.isEstimatedPower());
	}

}
