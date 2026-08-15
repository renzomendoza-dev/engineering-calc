package com.renzoproject.calc.core.acoustics;

/**
 * {@link #EXCEEDS_MAX_LIMIT} is deliberately distinct from {@link #FAIL} -- it's a design
 * conflict (the appliance is too loud, risking hearing damage), not an audibility shortfall, and
 * calls for a different remediation (reduce output or add sound treatment, not raise it).
 */
public enum AudibilityOutcome {
	PASS,
	FAIL,
	EXCEEDS_MAX_LIMIT
}
