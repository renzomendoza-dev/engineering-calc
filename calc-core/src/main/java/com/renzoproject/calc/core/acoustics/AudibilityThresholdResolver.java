package com.renzoproject.calc.core.acoustics;

/**
 * Resolves NFPA 72 audibility threshold rules for {@link FireAlarmAudibilityCalculator}. This is
 * a pre-step that runs before any calculation -- it does no dB math of its own, mirroring
 * {@code ConductorPropertiesResolver}'s separation of reference-data lookup from calculation.
 *
 * <p>Deliberately does not expose {@code averageAmbientSoundLevelByOccupancy}: that section of
 * {@code reference/acoustics/nfpa72-audibility-thresholds.json} is an unpopulated placeholder
 * (see its own {@code _action_required} note), not wired to anything yet.
 */
public interface AudibilityThresholdResolver {

	NotificationModeThreshold publicMode();

	NotificationModeThreshold privateMode();

	SleepingAreaThreshold sleepingArea();

	SystemWideLimits systemWideLimits();

}
