package com.renzoproject.calc.core.acoustics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Shape of the {@code notificationModes} object in
 * {@code reference/acoustics/nfpa72-audibility-thresholds.json}. Package-private -- internal to
 * {@link JsonAudibilityThresholdResolver}'s JSON parsing.
 *
 * <p>{@code public} and {@code private} are Java keywords, so the JSON's two keys (which use
 * exactly those strings) are mapped to differently-named record components via
 * {@code @JsonProperty}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record NotificationModesFile(
		@JsonProperty("public") NotificationModeThreshold publicMode,
		@JsonProperty("private") NotificationModeThreshold privateMode) {

}
