package com.renzoproject.calc.core.acoustics;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * General-occupant-notification input for {@link FireAlarmAudibilityCalculator}, evaluated
 * against {@link AudibilityThresholdResolver#publicMode()}.
 *
 * @param applianceSplAtReferenceDb    appliance's rated SPL at {@code referenceDistanceMeters},
 *                                     dB; must not be negative
 * @param referenceDistanceMeters      distance the rated SPL applies at, meters; must be positive
 * @param targetDistanceMeters         distance to the notification location, meters; must be
 *                                     positive
 * @param measuredAverageAmbientDb     measured average ambient sound level, dB; must not be
 *                                     negative
 * @param measuredMaxSustainedAmbientDb see {@link FireAlarmAudibilityInput#measuredMaxSustainedAmbientDb()}
 * @throws CalculationException if any of the above rules is violated
 */
public record PublicModeInput(
		double applianceSplAtReferenceDb,
		double referenceDistanceMeters,
		double targetDistanceMeters,
		double measuredAverageAmbientDb,
		Double measuredMaxSustainedAmbientDb) implements FireAlarmAudibilityInput {

	public PublicModeInput {
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
