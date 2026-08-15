package com.renzoproject.calc.core.acoustics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Shape of {@code reference/acoustics/nfpa72-audibility-thresholds.json}. Package-private --
 * internal to {@link JsonAudibilityThresholdResolver}'s JSON parsing.
 *
 * <p>{@code _meta} and {@code averageAmbientSoundLevelByOccupancy} (an unpopulated placeholder --
 * see that section's own {@code _action_required} note in the JSON) are deliberately not modeled
 * here; {@code @JsonIgnoreProperties} lets them pass through during deserialization without
 * needing a Java shape for either.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record NfpaAudibilityThresholdsFile(
		NotificationModesFile notificationModes,
		SleepingAreaThreshold sleepingArea,
		SystemWideLimits systemWideLimits) {

}
