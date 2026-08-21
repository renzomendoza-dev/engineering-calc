package com.renzoproject.calc_api.electrical.wiresizing;

import com.renzoproject.calc.core.electrical.wiresizing.WireSizingResult;

/**
 * HTTP response body for a wire sizing calculation, mirroring calc-core's
 * {@link WireSizingResult}.
 */
public record WireSizingResponse(
		String recommendedSizeLabel,
		double baseAmpacityAmps,
		double tempCorrectionFactor,
		double adjustmentFactor,
		double deratedAmpacityAmps,
		double requiredAmpacityAmps,
		int numberOfParallelSets,
		double requiredAmpacityPerSetAmps,
		boolean meetsTerminationRating,
		VoltageDropCheckResponseDto voltageDropCheckResult) {

	public static WireSizingResponse from(WireSizingResult result) {
		VoltageDropCheckResponseDto voltageDropCheckResponseDto = result.voltageDropCheckResult() == null
				? null
				: VoltageDropCheckResponseDto.from(result.voltageDropCheckResult());
		return new WireSizingResponse(
				result.recommendedSizeLabel(),
				result.baseAmpacityAmps(),
				result.tempCorrectionFactor(),
				result.adjustmentFactor(),
				result.deratedAmpacityAmps(),
				result.requiredAmpacityAmps(),
				result.numberOfParallelSets(),
				result.requiredAmpacityPerSetAmps(),
				result.meetsTerminationRating(),
				voltageDropCheckResponseDto);
	}

}
