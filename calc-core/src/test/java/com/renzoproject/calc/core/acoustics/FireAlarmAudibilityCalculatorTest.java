package com.renzoproject.calc.core.acoustics;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FireAlarmAudibilityCalculatorTest {

	private static final double DELTA = 1e-9;

	// Mirrors the published NFPA 72 rule values from nfpa72-audibility-thresholds.json.
	private final AudibilityThresholdResolver resolver = new FakeAudibilityThresholdResolver(
			new NotificationModeThreshold(15.0, 5.0, "greaterOf"),
			new NotificationModeThreshold(10.0, 5.0, "greaterOf"),
			new SleepingAreaThreshold(75.0, "absoluteFloor_overriddenByRelativeRuleIfGreater"),
			new SystemWideLimits(110.0));

	private final FireAlarmAudibilityCalculator calculator = new FireAlarmAudibilityCalculator(resolver);

	@Test
	void publicMode_workedExample_averageAmbient50_requiresThreshold65_maxSustainedOmitted() {
		// NFPA 72 worked example: average ambient 50 dB, public mode -> required threshold
		// 65 dB (50 + 15).
		PublicModeInput input = new PublicModeInput(65.0, 1.0, 1.0, 50.0, null);

		FireAlarmAudibilityResult result = calculator.calculate(input);

		assertEquals(65.0, result.requiredThresholdDb(), DELTA);
		assertEquals(GoverningAudibilityRule.AVERAGE_AMBIENT_PLUS_OFFSET, result.governingRule());
		assertEquals(AudibilityOutcome.PASS, result.outcome());
	}

	@Test
	void publicMode_workedExample_averageAmbient50_requiresThreshold65_maxSustainedBelowThreshold() {
		// Same worked example, but with a max-sustained reading (55 + 5 = 60) that's lower than
		// the average-derived threshold (65) -- average-ambient candidate should still govern.
		PublicModeInput input = new PublicModeInput(65.0, 1.0, 1.0, 50.0, 55.0);

		FireAlarmAudibilityResult result = calculator.calculate(input);

		assertEquals(65.0, result.requiredThresholdDb(), DELTA);
		assertEquals(GoverningAudibilityRule.AVERAGE_AMBIENT_PLUS_OFFSET, result.governingRule());
	}

	@Test
	void publicMode_maxSustainedCandidateHigher_governs() {
		// average candidate: 50+15=65; max-sustained candidate: 70+5=75 -- the greater wins.
		PublicModeInput input = new PublicModeInput(75.0, 1.0, 1.0, 50.0, 70.0);

		FireAlarmAudibilityResult result = calculator.calculate(input);

		assertEquals(75.0, result.requiredThresholdDb(), DELTA);
		assertEquals(GoverningAudibilityRule.MAX_SUSTAINED_PLUS_OFFSET, result.governingRule());
	}

	@Test
	void privateMode_usesNarrowerOffsets() {
		// average candidate: 50+10=60 (private mode's offset, not public's 15).
		PrivateModeInput input = new PrivateModeInput(60.0, 1.0, 1.0, 50.0, null);

		FireAlarmAudibilityResult result = calculator.calculate(input);

		assertEquals(60.0, result.requiredThresholdDb(), DELTA);
		assertEquals(GoverningAudibilityRule.AVERAGE_AMBIENT_PLUS_OFFSET, result.governingRule());
	}

	@Test
	void sleepingArea_lowAmbient_absoluteFloorGoverns() {
		// relative candidate: 40+15=55, below the 75 dB absolute pillow floor -- floor governs.
		SleepingAreaInput input = new SleepingAreaInput(75.0, 1.0, 1.0, 40.0, null);

		FireAlarmAudibilityResult result = calculator.calculate(input);

		assertEquals(75.0, result.requiredThresholdDb(), DELTA);
		assertEquals(GoverningAudibilityRule.ABSOLUTE_SLEEPING_FLOOR, result.governingRule());
	}

	@Test
	void sleepingArea_highAmbient_relativeRuleOverridesFloor() {
		// relative candidate: 65+15=80, above the 75 dB absolute pillow floor -- relative rule
		// (public mode's offsets) overrides the floor upward.
		SleepingAreaInput input = new SleepingAreaInput(80.0, 1.0, 1.0, 65.0, null);

		FireAlarmAudibilityResult result = calculator.calculate(input);

		assertEquals(80.0, result.requiredThresholdDb(), DELTA);
		assertEquals(GoverningAudibilityRule.AVERAGE_AMBIENT_PLUS_OFFSET, result.governingRule());
	}

	@Test
	void targetSplBelowThreshold_fails() {
		PublicModeInput input = new PublicModeInput(60.0, 1.0, 1.0, 50.0, null);

		FireAlarmAudibilityResult result = calculator.calculate(input);

		assertEquals(AudibilityOutcome.FAIL, result.outcome());
	}

	@Test
	void targetSplAboveMaximumAllowed_isDesignConflictNotFailure() {
		// 120 dB exceeds the 110 dB system-wide ceiling, even though it's also well above the
		// 65 dB required threshold -- EXCEEDS_MAX_LIMIT takes priority over PASS.
		PublicModeInput input = new PublicModeInput(120.0, 1.0, 1.0, 50.0, null);

		FireAlarmAudibilityResult result = calculator.calculate(input);

		assertEquals(AudibilityOutcome.EXCEEDS_MAX_LIMIT, result.outcome());
	}

	@Test
	void unsupportedNotificationModeRule_throws() {
		AudibilityThresholdResolver badResolver = new FakeAudibilityThresholdResolver(
				new NotificationModeThreshold(15.0, 5.0, "unknownRule"),
				new NotificationModeThreshold(10.0, 5.0, "greaterOf"),
				new SleepingAreaThreshold(75.0, "absoluteFloor_overriddenByRelativeRuleIfGreater"),
				new SystemWideLimits(110.0));
		FireAlarmAudibilityCalculator badCalculator = new FireAlarmAudibilityCalculator(badResolver);
		PublicModeInput input = new PublicModeInput(65.0, 1.0, 1.0, 50.0, null);

		assertThrows(CalculationException.class, () -> badCalculator.calculate(input));
	}

	@Test
	void unsupportedSleepingAreaRule_throws() {
		AudibilityThresholdResolver badResolver = new FakeAudibilityThresholdResolver(
				new NotificationModeThreshold(15.0, 5.0, "greaterOf"),
				new NotificationModeThreshold(10.0, 5.0, "greaterOf"),
				new SleepingAreaThreshold(75.0, "unknownRule"),
				new SystemWideLimits(110.0));
		FireAlarmAudibilityCalculator badCalculator = new FireAlarmAudibilityCalculator(badResolver);
		SleepingAreaInput input = new SleepingAreaInput(75.0, 1.0, 1.0, 40.0, null);

		assertThrows(CalculationException.class, () -> badCalculator.calculate(input));
	}

	@Test
	void negativeApplianceSpl_throws() {
		assertThrows(CalculationException.class, () -> new PublicModeInput(-1.0, 1.0, 1.0, 50.0, null));
	}

	@Test
	void nonPositiveReferenceDistance_throws() {
		assertThrows(CalculationException.class, () -> new PublicModeInput(65.0, 0.0, 1.0, 50.0, null));
		assertThrows(CalculationException.class, () -> new PublicModeInput(65.0, -1.0, 1.0, 50.0, null));
	}

	@Test
	void nonPositiveTargetDistance_throws() {
		assertThrows(CalculationException.class, () -> new PublicModeInput(65.0, 1.0, 0.0, 50.0, null));
		assertThrows(CalculationException.class, () -> new PublicModeInput(65.0, 1.0, -1.0, 50.0, null));
	}

	@Test
	void negativeMeasuredAverageAmbient_throws() {
		assertThrows(CalculationException.class, () -> new PublicModeInput(65.0, 1.0, 1.0, -1.0, null));
	}

	@Test
	void negativeMeasuredMaxSustainedAmbient_throws() {
		assertThrows(CalculationException.class, () -> new PublicModeInput(65.0, 1.0, 1.0, 50.0, -1.0));
	}

	@Test
	void privateModeAndSleepingAreaInput_shareSameValidation() {
		assertThrows(CalculationException.class, () -> new PrivateModeInput(-1.0, 1.0, 1.0, 50.0, null));
		assertThrows(CalculationException.class, () -> new SleepingAreaInput(-1.0, 1.0, 1.0, 50.0, null));
	}

}
