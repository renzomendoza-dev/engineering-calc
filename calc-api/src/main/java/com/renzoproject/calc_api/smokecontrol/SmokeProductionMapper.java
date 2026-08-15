package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.smokecontrol.FarField;
import com.renzoproject.calc.core.smokecontrol.NearField;
import com.renzoproject.calc.core.smokecontrol.PlumeRegime;
import com.renzoproject.calc.core.smokecontrol.SmokeProductionInput;
import com.renzoproject.calc.core.smokecontrol.SmokeProductionResult;

/** Pure mapping, no logic — matches every other mapper in this codebase. */
public final class SmokeProductionMapper {

	private SmokeProductionMapper() {
	}

	public static SmokeProductionInput toCoreInput(SmokeProductionRequest request) {
		return new SmokeProductionInput(
				request.designFireArea(),
				request.heatReleaseRateDensity(),
				request.convectiveFraction(),
				request.ceilingHeight(),
				request.fireBaseHeight(),
				request.ambientTemperature(),
				request.fractionConvectiveHeatInSmokeLayer());
	}

	public static SmokeProductionResponse toResponse(SmokeProductionResult result) {
		return new SmokeProductionResponse(
				result.designHeatReleaseRate(),
				result.convectiveHeatReleaseRate(),
				result.flameHeight(),
				result.heightAboveFire(),
				toDtoPlumeRegime(result.plumeRegime()),
				result.smokeTemperature(),
				result.smokeDensity(),
				result.volumetricFlowRate());
	}

	private static PlumeRegimeDto toDtoPlumeRegime(PlumeRegime plumeRegime) {
		return switch (plumeRegime) {
			case FarField farField -> new PlumeRegimeDto(PlumeRegimeTypeDto.FAR_FIELD, farField.massFlowRateKgS());
			case NearField nearField -> new PlumeRegimeDto(PlumeRegimeTypeDto.NEAR_FIELD, nearField.massFlowRateKgS());
		};
	}

}
