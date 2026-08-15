package com.renzoproject.calc.core.acoustics;

/**
 * Which notification-mode threshold family applies to a {@link FireAlarmAudibilityCalculator}
 * evaluation -- a sealed interface rather than an enum-discriminated flat record (mirroring
 * {@code FirePumpPowerInput}/{@code DiameterSpec}'s pattern), since the calculator needs to
 * pattern-match on mode to resolve the applicable rule from {@link AudibilityThresholdResolver}.
 *
 * <p>Unlike those two examples, all three permitted records share an identical field shape --
 * there's no genuinely different data per mode, only a different threshold rule to apply -- so
 * (unusually for this codebase's sealed interfaces) the shared accessors are declared here
 * instead of being re-extracted per {@code switch} arm, since every implementation already
 * provides them for free as record components.
 */
public sealed interface FireAlarmAudibilityInput permits PublicModeInput, PrivateModeInput, SleepingAreaInput {

	double applianceSplAtReferenceDb();

	double referenceDistanceMeters();

	double targetDistanceMeters();

	double measuredAverageAmbientDb();

	/**
	 * 60-second-max sustained ambient reading, dB. Nullable: the on-site measurement window this
	 * requires isn't always available. When {@code null}, {@link FireAlarmAudibilityCalculator}
	 * evaluates only the average-ambient-based candidate ({@code aboveAverageAmbientDb}) for the
	 * "greater of" comparison -- not treated as a candidate of {@code 0}, which would never
	 * govern and would silently hide the fact that the max-sustained rule was never checked.
	 */
	Double measuredMaxSustainedAmbientDb();

}
