package com.renzoproject.calc.core.electrical.voltagedrop;

/**
 * Result of a {@link VoltageDropCalculator} calculation.
 *
 * @param voltageDropVolts         voltage lost over the run, in volts
 * @param voltageDropPercent       voltage drop as a percentage of {@code systemVoltage}
 * @param receivingEndVoltage      voltage remaining at the load end, in volts
 *                                 ({@code systemVoltage - voltageDropVolts})
 * @param exceedsRecommendedLimit  {@code true} if {@code voltageDropPercent} exceeds
 *                                 {@link VoltageDropCalculator#RECOMMENDED_MAX_PERCENT}
 *                                 (informational only — this is never thrown as an error)
 */
public record VoltageDropResult(
		double voltageDropVolts,
		double voltageDropPercent,
		double receivingEndVoltage,
		boolean exceedsRecommendedLimit) {

}
