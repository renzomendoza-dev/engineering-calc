package com.renzoproject.calc.core.acoustics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link JsonAudibilityThresholdResolver} against the real
 * {@code nfpa72-audibility-thresholds.json} reference data.
 */
class JsonAudibilityThresholdResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonAudibilityThresholdResolver resolver = new JsonAudibilityThresholdResolver();

	@Test
	void publicMode_matchesPublishedValues() {
		NotificationModeThreshold publicMode = resolver.publicMode();

		assertEquals(15.0, publicMode.aboveAverageAmbientDb(), DELTA);
		assertEquals(5.0, publicMode.aboveMaxSustainedDb(), DELTA);
		assertEquals("greaterOf", publicMode.rule());
	}

	@Test
	void privateMode_matchesPublishedValues() {
		NotificationModeThreshold privateMode = resolver.privateMode();

		assertEquals(10.0, privateMode.aboveAverageAmbientDb(), DELTA);
		assertEquals(5.0, privateMode.aboveMaxSustainedDb(), DELTA);
		assertEquals("greaterOf", privateMode.rule());
	}

	@Test
	void sleepingArea_matchesPublishedValues() {
		SleepingAreaThreshold sleepingArea = resolver.sleepingArea();

		assertEquals(75.0, sleepingArea.minimumDbaAtPillow(), DELTA);
		assertEquals("absoluteFloor_overriddenByRelativeRuleIfGreater", sleepingArea.rule());
	}

	@Test
	void systemWideLimits_matchesPublishedValues() {
		assertEquals(110.0, resolver.systemWideLimits().maximumAllowedDba(), DELTA);
	}

}
