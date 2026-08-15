package com.renzoproject.calc.core.acoustics;

/**
 * Result of {@link FireAlarmAudibilityCalculator}. Deliberately doesn't echo the input fields
 * (matching every other calc-core {@code Result} record -- none of them echo their inputs; a
 * caller already has the {@code Input} instance it built, in scope alongside this result).
 *
 * @param calculatedTargetSplDb sound pressure level at the target location, via
 *                               {@link com.renzoproject.calc.core.acoustics.AcousticsMath#distanceAttenuationDb}
 * @param requiredThresholdDb   the greater of the applicable candidate thresholds
 * @param governingRule         which candidate threshold governed {@code requiredThresholdDb} --
 *                               required for a pass/fail explanation to say why, not just what
 * @param outcome                {@link AudibilityOutcome#EXCEEDS_MAX_LIMIT} is distinct from
 *                               {@link AudibilityOutcome#FAIL}; see that enum's Javadoc
 */
public record FireAlarmAudibilityResult(
		double calculatedTargetSplDb,
		double requiredThresholdDb,
		GoverningAudibilityRule governingRule,
		AudibilityOutcome outcome) {

}
