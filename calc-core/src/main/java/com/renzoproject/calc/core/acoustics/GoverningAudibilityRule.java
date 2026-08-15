package com.renzoproject.calc.core.acoustics;

/**
 * Which of the candidate threshold rules governed {@link FireAlarmAudibilityCalculator}'s
 * {@code requiredThresholdDb} -- surfaced so a pass/fail explanation can say why, not just what.
 */
public enum GoverningAudibilityRule {
	AVERAGE_AMBIENT_PLUS_OFFSET,
	MAX_SUSTAINED_PLUS_OFFSET,
	ABSOLUTE_SLEEPING_FLOOR
}
