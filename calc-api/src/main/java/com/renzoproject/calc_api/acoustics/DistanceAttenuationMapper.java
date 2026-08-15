package com.renzoproject.calc_api.acoustics;

import com.renzoproject.calc.core.acoustics.DistanceAttenuationInput;
import com.renzoproject.calc.core.acoustics.DistanceAttenuationResult;

/** Pure mapping, no logic — matches every other mapper in this codebase. */
public final class DistanceAttenuationMapper {

	private DistanceAttenuationMapper() {
	}

	public static DistanceAttenuationInput toCoreInput(DistanceAttenuationRequest request) {
		return new DistanceAttenuationInput(request.referenceSplDb(), request.referenceDistance(), request.targetDistance());
	}

	public static DistanceAttenuationResponse toResponse(DistanceAttenuationResult result) {
		return new DistanceAttenuationResponse(result.targetSplDb(), result.attenuationDb());
	}

}
