package com.renzoproject.calc_api.electrical.wiresizing;

import com.renzoproject.calc.core.electrical.wiresizing.VoltageDropCheckResult;

/**
 * HTTP response representation of calc-core's {@link VoltageDropCheckResult}.
 */
public record VoltageDropCheckResponseDto(
		String sizeLabelChecked,
		double voltageDropPercent,
		boolean exceedsRecommendedLimit,
		String upsizedRecommendation) {

	public static VoltageDropCheckResponseDto from(VoltageDropCheckResult result) {
		return new VoltageDropCheckResponseDto(
				result.sizeLabelChecked(),
				result.voltageDropPercent(),
				result.exceedsRecommendedLimit(),
				result.upsizedRecommendation());
	}

}
