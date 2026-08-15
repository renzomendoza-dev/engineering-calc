package com.renzoproject.calc_api.acoustics;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * HTTP request body for a fire alarm audibility calculation. A single flat DTO with a
 * {@code mode} discriminator, mirroring {@code VoltageDropRequest}'s mode-switch pattern --
 * except no cross-field {@code @AssertTrue} validator is needed here, since (unlike
 * {@code VoltageDropRequest}'s {@code useCustomImpedance} switch) all three modes share
 * identical fields; only {@link FireAlarmAudibilityMapper} needs to branch on {@code mode}, to
 * pick which calc-core sealed-interface subtype to build.
 *
 * <p>Bean Validation here is layered on top of, not instead of, calc-core's own manual checks
 * in {@code PublicModeInput}/{@code PrivateModeInput}/{@code SleepingAreaInput}'s compact
 * constructors -- same convention as {@code DistanceAttenuationRequest}.
 *
 * <p>{@code measuredMaxSustainedAmbientDb} has no {@code @NotNull} -- calc-core's
 * {@code FireAlarmAudibilityInput} explicitly allows it to be omitted (the 60-second-max
 * measurement window isn't always available on-site), in which case only the average-ambient
 * candidate is evaluated.
 */
public record FireAlarmAudibilityRequest(
		@NotNull FireAlarmNotificationMode mode,
		@NotNull @PositiveOrZero Double applianceSplAtReferenceDb,
		@NotNull @Positive Double referenceDistanceMeters,
		@NotNull @Positive Double targetDistanceMeters,
		@NotNull @PositiveOrZero Double measuredAverageAmbientDb,
		@PositiveOrZero Double measuredMaxSustainedAmbientDb) {

}
