package com.renzoproject.calc.core.smokecontrol;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.common.AirProperties;
import com.renzoproject.calc.core.common.AirPropertiesResolver;

/**
 * NFPA 92 Heskestad axisymmetric plume correlation: smoke mass/volumetric production rate from a
 * design fire's heat release rate, ceiling height, and ambient conditions.
 */
public class SmokeProductionCalculator implements Calculator<SmokeProductionInput, SmokeProductionResult> {

	private static final double FLAME_HEIGHT_COEFFICIENT = 0.166;
	private static final double FAR_FIELD_ENTRAINMENT_COEFFICIENT = 0.071;
	private static final double FAR_FIELD_RADIATIVE_LOSS_COEFFICIENT = 0.0018;
	private static final double NEAR_FIELD_ENTRAINMENT_COEFFICIENT = 0.032;
	private static final double CELSIUS_TO_KELVIN_OFFSET = 273.0;

	private final AirPropertiesResolver airPropertiesResolver;
	private final SmokeControlDefaultsResolver defaultsResolver;

	public SmokeProductionCalculator(AirPropertiesResolver airPropertiesResolver, SmokeControlDefaultsResolver defaultsResolver) {
		this.airPropertiesResolver = airPropertiesResolver;
		this.defaultsResolver = defaultsResolver;
	}

	@Override
	public SmokeProductionResult calculate(SmokeProductionInput input) {
		SmokeControlDefaults defaults = defaultsResolver.defaults();
		double convectiveFraction = input.convectiveFraction() != null ? input.convectiveFraction() : defaults.convectiveFraction();
		double ksFraction = input.fractionConvectiveHeatInSmokeLayer() != null
				? input.fractionConvectiveHeatInSmokeLayer()
				: defaults.fractionConvectiveHeatInSmokeLayer();

		double designHeatReleaseRate = input.designFireArea() * input.heatReleaseRateDensity();
		double convectiveHeatReleaseRate = convectiveFraction * designHeatReleaseRate;
		double flameHeight = FLAME_HEIGHT_COEFFICIENT * Math.pow(convectiveHeatReleaseRate, 2.0 / 5.0);
		double heightAboveFire = input.ceilingHeight() - input.fireBaseHeight();

		PlumeRegime plumeRegime = heightAboveFire > flameHeight
				? new FarField(farFieldMassFlowRateKgS(convectiveHeatReleaseRate, heightAboveFire))
				: new NearField(nearFieldMassFlowRateKgS(convectiveHeatReleaseRate, heightAboveFire));

		AirProperties air = airPropertiesResolver.properties();
		double massFlowRateKgS = plumeRegime.massFlowRateKgS();
		double smokeTemperature = input.ambientTemperature()
				+ (ksFraction * convectiveHeatReleaseRate) / (massFlowRateKgS * air.specificHeatKjPerKgK());
		double smokeDensity = air.atmosphericPressurePa()
				/ (air.specificGasConstantJPerKgK() * (smokeTemperature + CELSIUS_TO_KELVIN_OFFSET));
		double volumetricFlowRate = massFlowRateKgS / smokeDensity;

		return new SmokeProductionResult(
				designHeatReleaseRate,
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
