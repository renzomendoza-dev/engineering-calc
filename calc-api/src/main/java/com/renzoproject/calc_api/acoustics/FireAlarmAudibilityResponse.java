package com.renzoproject.calc_api.acoustics;

/**
 * HTTP response body for a fire alarm audibility calculation. Mirrors calc-core's
 * {@code FireAlarmAudibilityResult} exactly -- no echoed request fields, consistent with every
 * other calc-api response DTO in this codebase (e.g. {@code DistanceAttenuationResponse}), none
 * of which echo their request's inputs either.
 */
public record FireAlarmAudibilityResponse(
		Double calculatedTargetSplDb,
		Double requiredThresholdDb,
		GoverningAudibilityRuleDto governingRule,
		AudibilityOutcomeDto outcome) {

}
