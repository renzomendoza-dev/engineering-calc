package com.renzoproject.calc_api.acoustics;

/**
 * Request-layer discriminator selecting which calc-core
 * {@code FireAlarmAudibilityInput} sealed-interface subtype to construct. calc-core has no
 * equivalent enum of its own -- it discriminates via the sealed interface's three record types
 * directly -- so this exists purely to carry the mode over HTTP, mirrored to a
 * {@code PublicModeInput}/{@code PrivateModeInput}/{@code SleepingAreaInput} choice by
 * {@link FireAlarmAudibilityMapper}.
 */
public enum FireAlarmNotificationMode {
	PUBLIC,
	PRIVATE,
	SLEEPING
}
