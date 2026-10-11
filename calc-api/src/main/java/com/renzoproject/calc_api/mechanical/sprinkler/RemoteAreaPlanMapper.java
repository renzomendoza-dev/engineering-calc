package com.renzoproject.calc_api.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.sprinkler.RemoteAreaPlanInput;
import com.renzoproject.calc.core.mechanical.sprinkler.RemoteAreaPlanResult;

/**
 * Pure mapping, no logic -- matches every other mapper in this codebase. No unit conversion:
 * calc-core's sprinkler package already works in NFPA 13's units and so does this endpoint.
 */
public final class RemoteAreaPlanMapper {

	private RemoteAreaPlanMapper() {
	}

	public static RemoteAreaPlanInput toCoreInput(RemoteAreaPlanRequest request) {
		return new RemoteAreaPlanInput(
				request.designAreaSqFt(),
				request.densityGpmPerSqFt(),
				request.kFactor(),
				request.sprinklerSpacingFt(),
				request.branchSpacingFt(),
				request.quickResponseSprinklers(),
				request.ceilingHeightFt(),
				request.maxCoveragePerSprinklerSqFt());
	}

	public static RemoteAreaPlanResponse toResponse(RemoteAreaPlanResult result) {
		return RemoteAreaPlanResponse.from(result);
	}

}
