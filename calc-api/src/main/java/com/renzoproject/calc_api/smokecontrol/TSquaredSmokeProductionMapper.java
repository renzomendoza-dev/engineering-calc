package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.smokecontrol.TSquaredFarField;
import com.renzoproject.calc.core.smokecontrol.TSquaredNearField;
import com.renzoproject.calc.core.smokecontrol.TSquaredPlumeRegime;
import com.renzoproject.calc.core.smokecontrol.TSquaredSmokeProductionInput;
import com.renzoproject.calc.core.smokecontrol.TSquaredSmokeProductionResult;

/**
 * Pure mapping, no logic -- deliberately separate from {@link SmokeProductionMapper}, not sharing
 * logic, matching the "fully separate, duplicated" decision already made at the core layer.
 */
public final class TSquaredSmokeProductionMapper {

	private TSquaredSmokeProductionMapper() {
	}

	public static TSquaredSmokeProductionInput toCoreInput(TSquaredSmokeProductionRequest request) {
		return new TSquaredSmokeProductionInput(
				request.fireGrowthRate(),
				request.cappingHRR(),
				request.evaluationTime(),
				request.convectiveFraction(),
				request.ceilingHeight(),
				request.fireBaseHeight(),
				request.ambientTemperature(),
				request.fractionConvectiveHeatInSmokeLayer());
	}

	public static TSquaredSmokeProductionResponse toResponse(TSquaredSmokeProductionResult result) {
		return new TSquaredSmokeProductionResponse(
				result.evaluationTime(),
				result.designHeatReleaseRate(),
				result.isGrowthCapped(),
				result.convectiveHeatReleaseRate(),
				result.flameHeight(),
				result.heightAboveFire(),
				toDtoPlumeRegime(result.plumeRegime()),
				result.smokeTemperature(),
				result.smokeDensity(),
				result.volumetricFlowRate());
	}

	private static TSquaredPlumeRegimeDto toDtoPlumeRegime(TSquaredPlumeRegime plumeRegime) {
		return switch (plumeRegime) {
			case TSquaredFarField farField -> new TSquaredPlumeRegimeDto(TSquaredPlumeRegimeTypeDto.FAR_FIELD, farField.massFlowRateKgS());
			case TSquaredNearField nearField -> new TSquaredPlumeRegimeDto(TSquaredPlumeRegimeTypeDto.NEAR_FIELD, nearField.massFlowRateKgS());
		};
	}

}
