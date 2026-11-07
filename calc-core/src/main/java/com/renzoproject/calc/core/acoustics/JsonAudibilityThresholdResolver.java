package com.renzoproject.calc.core.acoustics;

import com.renzoproject.calc.core.reference.ReferenceJson;

/**
 * Loads {@code reference/acoustics/nfpa72-audibility-thresholds.json} from the classpath once.
 * Mirrors {@code JsonFirePumpMotorSizeResolver}'s loading approach: read through {@code ReferenceJson},
 * hardcoded resource path constant, {@code CalculationException} on a missing/unreadable
 * resource, parsed eagerly in the constructor and cached in fields rather than re-read per call.
 */
public class JsonAudibilityThresholdResolver implements AudibilityThresholdResolver {

	private static final String RESOURCE_PATH = "/reference/acoustics/nfpa72-audibility-thresholds.json";

	private final NotificationModeThreshold publicMode;
	private final NotificationModeThreshold privateMode;
	private final SleepingAreaThreshold sleepingArea;
	private final SystemWideLimits systemWideLimits;

	public JsonAudibilityThresholdResolver() {
		NfpaAudibilityThresholdsFile file = load();
		this.publicMode = file.notificationModes().publicMode();
		this.privateMode = file.notificationModes().privateMode();
		this.sleepingArea = file.sleepingArea();
		this.systemWideLimits = file.systemWideLimits();
	}

	private static NfpaAudibilityThresholdsFile load() {
		return ReferenceJson.read(RESOURCE_PATH, NfpaAudibilityThresholdsFile.class);
	}

	@Override
	public NotificationModeThreshold publicMode() {
		return publicMode;
	}

	@Override
	public NotificationModeThreshold privateMode() {
		return privateMode;
	}

	@Override
	public SleepingAreaThreshold sleepingArea() {
		return sleepingArea;
	}

	@Override
	public SystemWideLimits systemWideLimits() {
		return systemWideLimits;
	}

}
