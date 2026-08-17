package com.renzoproject.calc.core.smokecontrol;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.common.AirProperties;
import com.renzoproject.calc.core.common.AirPropertiesResolver;

/**
 * NFPA 92 Heskestad axisymmetric plume correlation, driven by a t-squared design fire (growth
 * rate capped at a maximum HRR) evaluated at a single time point, rather than
 * {@link SmokeProductionCalculator}'s fixed area/HRR-density design fire.
 *
 * <p>Deliberately NOT a refactor of {@link SmokeProductionCalculator} -- steps 2-8 of the plume
 * correlation are identical NFPA 92 math, duplicated here on purpose (see
 * {@link TSquaredPlumeRegime}) rather than shared, so the two calculators can evolve
 * independently without one's growth-model change risking the other. {@link AirPropertiesResolver}
 * and {@link SmokeControlDefaultsResolver} ARE reused as-is, since those are genuinely
 * calculator-agnostic reference data, not part of the duplicated calculation logic.
 *
 * <p>The smoke-temperature formula below is deliberately the plain
 * {@code To + (Ks * Qc) / (m * Cp)} form, with no extra multiplier on the {@code Qc} term -- a
 * reference workbook this feature was modeled on applies an undocumented {@code x0.5} factor at
 * this exact spot, which is a transcription bug, not part of the validated NFPA 92 formula (and
 * not what {@link SmokeProductionCalculator} implements either).
 */
public class TSquaredSmokeProductionCalculator implements Calculator<TSquaredSmokeProductionInput, TSquaredSmokeProductionResult> {

	private static final double FLAME_HEIGHT_COEFFICIENT = 0.166;
	private static final double FAR_FIELD_ENTRAINMENT_COEFFICIENT = 0.071;
	private static final double FAR_FIELD_RADIATIVE_LOSS_COEFFICIENT = 0.0018;
	private static final double NEAR_FIELD_ENTRAINMENT_COEFFICIENT = 0.032;
	private static final double CELSIUS_TO_KELVIN_OFFSET = 273.0;

	private final AirPropertiesResolver airPropertiesResolver;
	private final SmokeControlDefaultsResolver defaultsResolver;

	public TSquaredSmokeProductionCalculator(AirPropertiesResolver airPropertiesResolver, SmokeControlDefaultsResolver defaultsResolver) {
		this.airPropertiesResolver = airPropertiesResolver;
		this.defaultsResolver = defaultsResolver;
	}

	@Override
	public TSquaredSmokeProductionResult calculate(TSquaredSmokeProductionInput input) {
		SmokeControlDefaults defaults = defaultsResolver.defaults();
		double convectiveFraction = input.convectiveFraction() != null ? input.convectiveFraction() : defaults.convectiveFraction();
		double ksFraction = input.fractionConvectiveHeatInSmokeLayer() != null
				? input.fractionConvectiveHeatInSmokeLayer()
				: defaults.fractionConvectiveHeatInSmokeLayer();

		double uncappedHeatReleaseRate = input.fireGrowthRate() * input.evaluationTime() * input.evaluationTime();
		boolean isGrowthCapped = uncappedHeatReleaseRate >= input.cappingHRR();
		double designHeatReleaseRate = Math.min(uncappedHeatReleaseRate, input.cappingHRR());

		double convectiveHeatReleaseRate = convectiveFraction * designHeatReleaseRate;
		double flameHeight = FLAME_HEIGHT_COEFFICIENT * Math.pow(convectiveHeatReleaseRate, 2.0 / 5.0);
		double heightAboveFire = input.ceilingHeight() - input.fireBaseHeight();

		TSquaredPlumeRegime plumeRegime = heightAboveFire > flameHeight
				? new TSquaredFarField(farFieldMassFlowRateKgS(convectiveHeatReleaseRate, heightAboveFire))
				: new TSquaredNearField(nearFieldMassFlowRateKgS(convectiveHeatReleaseRate, heightAboveFire));

		AirProperties air = airPropertiesResolver.properties();
		double massFlowRateKgS = plumeRegime.massFlowRateKgS();
		double smokeTemperature = input.ambientTemperature()
				+ (ksFraction * convectiveHeatReleaseRate) / (massFlowRateKgS * air.specificHeatKjPerKgK());
		double smokeDensity = air.atmosphericPressurePa()
				/ (air.specificGasConstantJPerKgK() * (smokeTemperature + CELSIUS_TO_KELVIN_OFFSET));
		double volumetricFlowRate = massFlowRateKgS / smokeDensity;

		return new TSquaredSmokeProductionResult(
				input.evaluationTime(),
				designHeatReleaseRate,
				isGrowthCapped,
				convectiveHeatReleaseRate,
				flameHeight,
				heightAboveFire,
				plumeRegime,
				smokeTemperature,
				smokeDensity,
				volumetricFlowRate);
	}

	private static double farFieldMassFlowRateKgS(double convectiveHeatReleaseRate, double heightAboveFire) {
		return FAR_FIELD_ENTRAINMENT_COEFFICIENT * Math.pow(convectiveHeatReleaseRate, 1.0 / 3.0) * Math.pow(heightAboveFire, 5.0 / 3.0)
				+ FAR_FIELD_RADIATIVE_LOSS_COEFFICIENT * convectiveHeatReleaseRate;
	}

	private static double nearFieldMassFlowRateKgS(double convectiveHeatReleaseRate, double heightAboveFire) {
		return NEAR_FIELD_ENTRAINMENT_COEFFICIENT * Math.pow(convectiveHeatReleaseRate, 3.0 / 5.0) * heightAboveFire;
	}

}
