package com.renzoproject.calc.core.acoustics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads {@code reference/acoustics/nfpa72-audibility-thresholds.json} from the classpath once.
 * Mirrors {@code JsonFirePumpMotorSizeResolver}'s loading approach: fresh {@code ObjectMapper},
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
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonAudibilityThresholdResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, NfpaAudibilityThresholdsFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
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
