package com.renzoproject.calc.core.smokecontrol;

/**
 * Result of {@link TSquaredSmokeProductionCalculator}. Deliberate duplicate of
 * {@link SmokeProductionResult}'s "expose every intermediate" shape, plus the growth-specific
 * {@code evaluationTime}/{@code isGrowthCapped} fields that have no equivalent there.
 *
 * @param evaluationTime            t, seconds -- echoed since it's the one point this result was
 *                                   evaluated at, not just an input the caller already has (the
 *                                   value that actually drove {@code designHeatReleaseRate})
 * @param designHeatReleaseRate     Q, kW -- {@code min(fireGrowthRate * evaluationTime^2, cappingHRR)}
 * @param isGrowthCapped            {@code true} when the uncapped t-squared growth value at
 *                                   {@code evaluationTime} is at or past {@code cappingHRR} (i.e.
 *                                   {@code designHeatReleaseRate} came from the cap, not the
 *                                   growth curve)
 * @param convectiveHeatReleaseRate Qc, kW
 * @param flameHeight               z_l, m
 * @param heightAboveFire           z, m
 * @param plumeRegime                which correlation governed, carrying its own mass flow
 *                                   result ({@code m}, kg/s) -- see {@link TSquaredPlumeRegime}
 * @param smokeTemperature          Ts, degC
 * @param smokeDensity              rho, kg/m3
 * @param volumetricFlowRate        v, m3/s
 */
public record TSquaredSmokeProductionResult(
		double evaluationTime,
		double designHeatReleaseRate,
		boolean isGrowthCapped,
		double convectiveHeatReleaseRate,
		double flameHeight,
		double heightAboveFire,
		TSquaredPlumeRegime plumeRegime,
		double smokeTemperature,
		double smokeDensity,
		double volumetricFlowRate) {

}
