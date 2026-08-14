package com.renzoproject.calc_api.electrical.conduitfill;

import com.renzoproject.calc.core.electrical.conduitfill.ConduitFillResult;

/**
 * HTTP response body for a conduit fill calculation, mirroring calc-core's
 * {@link ConduitFillResult}.
 *
 * <p>{@code practicalFillAdvisory} is a separate field-experience pull-ease note — see
 * {@link PracticalFillAdvisoryDto} — kept as its own nested object rather than flattened in,
 * since it is explicitly NOT a PEC compliance result and must not be mistaken for one.
 */
public record ConduitFillResponse(
		String recommendedTradeSizeMm,
		double totalConductorAreaMm2,
		int totalConductorCount,
		double allowedFillPercent,
		Double actualFillPercentAtRecommendedSize,
		boolean requiresMultipleConduits,
		PracticalFillAdvisoryDto practicalFillAdvisory) {

	public static ConduitFillResponse from(ConduitFillResult result) {
		PracticalFillAdvisoryDto advisoryDto = result.practicalFillAdvisory() == null
				? null
				: PracticalFillAdvisoryDto.from(result.practicalFillAdvisory());
		return new ConduitFillResponse(
				result.recommendedTradeSizeMm(),
				result.totalConductorAreaMm2(),
				result.totalConductorCount(),
				result.allowedFillPercent(),
				result.actualFillPercentAtRecommendedSize(),
				result.requiresMultipleConduits(),
				advisoryDto);
	}

}
