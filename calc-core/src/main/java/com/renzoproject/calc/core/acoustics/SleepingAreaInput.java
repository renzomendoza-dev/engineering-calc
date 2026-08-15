package com.renzoproject.calc.core.acoustics;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Sleeping-area input for {@link FireAlarmAudibilityCalculator}, evaluated against the greater of
 * {@link AudibilityThresholdResolver#sleepingArea()}'s absolute pillow floor and
 * {@link AudibilityThresholdResolver#publicMode()}'s relative rule -- see
 * {@link FireAlarmAudibilityCalculator} for why the public (not private) relative rule is the one
 * used here.
 *
 * @param applianceSplAtReferenceDb    appliance's rated SPL at {@code referenceDistanceMeters},
 *                                     dB; must not be negative
 * @param referenceDistanceMeters      distance the rated SPL applies at, meters; must be positive
 * @param targetDistanceMeters         distance to the notification location (e.g. at the pillow),
 *                                     meters; must be positive
 * @param measuredAverageAmbientDb     measured average ambient sound level, dB; must not be
 *                                     negative
 * @param measuredMaxSustainedAmbientDb see {@link FireAlarmAudibilityInput#measuredMaxSustainedAmbientDb()}
 * @throws CalculationException if any of the above rules is violated
 */
public record SleepingAreaInput(
		double applianceSplAtReferenceDb,
		double referenceDistanceMeters,
		double targetDistanceMeters,
		double measuredAverageAmbientDb,
		Double measuredMaxSustainedAmbientDb) implements FireAlarmAudibilityInput {

	public SleepingAreaInput {
		if (applianceSplAtReferenceDb < 0) {
			throw new CalculationException("applianceSplAtReferenceDb must not be negative");
		}
		if (referenceDistanceMeters <= 0) {
			throw new CalculationException("referenceDistanceMeters must be positive");
		}
		if (targetDistanceMeters <= 0) {
			throw new CalculationException("targetDistanceMeters must be positive");
		}
		if (measuredAverageAmbientDb < 0) {
			throw new CalculationException("measuredAverageAmbientDb must not be negative");
		}
		if (measuredMaxSustainedAmbientDb != null && measuredMaxSustainedAmbientDb < 0) {
			throw new CalculationException("measuredMaxSustainedAmbientDb must not be negative");
		}
	}

}
