package com.renzoproject.calc.core.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads {@code reference/common/air-properties.json} from the classpath once. Mirrors
 * {@code JsonFirePumpMotorSizeResolver}'s loading approach: fresh {@code ObjectMapper},
 * hardcoded resource path constant, {@code CalculationException} on a missing/unreadable
 * resource, parsed eagerly in the constructor and cached rather than re-read per call.
 */
public class JsonAirPropertiesResolver implements AirPropertiesResolver {

	private static final String RESOURCE_PATH = "/reference/common/air-properties.json";

	private final AirProperties properties;

	public JsonAirPropertiesResolver() {
		AirPropertiesFile file = load();
		this.properties = new AirProperties(
				file.properties().specificHeat().value(),
				file.properties().atmosphericPressure().value(),
				file.properties().specificGasConstant().value());
	}

	private static AirPropertiesFile load() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonAirPropertiesResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, AirPropertiesFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public AirProperties properties() {
		return properties;
	}

}
