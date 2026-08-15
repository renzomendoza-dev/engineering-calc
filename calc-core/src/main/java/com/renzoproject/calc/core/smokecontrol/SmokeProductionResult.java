package com.renzoproject.calc.core.smokecontrol;

/**
 * Result of {@link SmokeProductionCalculator}. Unlike most calc-core {@code Result} records, this
 * one deliberately exposes every intermediate value from the NFPA 92 calculation sequence, not
 * just the final {@code volumetricFlowRate} -- mirroring the source design document's calculation
 * sheet, where each step is shown, since the eventual API/web layers are expected to surface all
 * of them, not just the final answer.
 *
 * @param designHeatReleaseRate     Q, kW
 * @param convectiveHeatReleaseRate Qc, kW
 * @param flameHeight               z_l, m
 * @param heightAboveFire           z, m
 * @param plumeRegime               which correlation governed, carrying its own mass flow result
 *                                  ({@code m}, kg/s) -- see {@link PlumeRegime}
 * @param smokeTemperature          Ts, degC
 * @param smokeDensity              rho, kg/m3
 * @param volumetricFlowRate        v, m3/s
 */
public record SmokeProductionResult(
		double designHeatReleaseRate,
		double convectiveHeatReleaseRate,
		double flameHeight,
		double heightAboveFire,
		PlumeRegime plumeRegime,
		double smokeTemperature,
		double smokeDensity,
		double volumetricFlowRate) {

}
