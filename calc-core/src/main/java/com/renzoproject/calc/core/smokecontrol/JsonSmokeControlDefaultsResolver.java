package com.renzoproject.calc.core.smokecontrol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads {@code reference/smoke-control/defaults.json} from the classpath once. Mirrors
 * {@code JsonFirePumpMotorSizeResolver}'s loading approach: fresh {@code ObjectMapper},
 * hardcoded resource path constant, {@code CalculationException} on a missing/unreadable
 * resource, parsed eagerly in the constructor and cached rather than re-read per call.
 */
public class JsonSmokeControlDefaultsResolver implements SmokeControlDefaultsResolver {

	private static final String RESOURCE_PATH = "/reference/smoke-control/defaults.json";

	private final SmokeControlDefaults defaults;

	public JsonSmokeControlDefaultsResolver() {
		SmokeControlDefaultsFile file = load();
		this.defaults = new SmokeControlDefaults(
				file.defaults().fractionConvectiveHeatInSmokeLayer().value(),
				file.defaults().convectiveFraction().value());
	}

	private static SmokeControlDefaultsFile load() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonSmokeControlDefaultsResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, SmokeControlDefaultsFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public SmokeControlDefaults defaults() {
		return defaults;
	}

}
