package com.renzoproject.calc.core.electrical.powerconsumption;

/**
 * Result of {@link PowerConsumptionCalculator}.
 *
 * @param powerWatts        resolved power draw, in watts
 * @param powerKw           {@code powerWatts / 1000}
 * @param energyConsumedKwh {@code powerKw * totalOperatingHours}
 * @param estimatedCost     {@code energyConsumedKwh * pricePerKwh}, or {@code null} if
 *                          {@code pricePerKwh} wasn't provided in the input
 * @param isEstimatedPower  {@code true} only when the input mode was
 *                          {@link PowerInputMode#HORSEPOWER}, since that mode depends on an
 *                          assumed or provided efficiency rather than a directly
 *                          measured/nameplate value; {@code false} for the other two modes,
 *                          which are exact given their inputs
 */
public record PowerConsumptionResult(
		double powerWatts,
		double powerKw,
		double energyConsumedKwh,
		Double estimatedCost,
		boolean isEstimatedPower) {

}
