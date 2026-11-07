package com.renzoproject.calc.core.common;

import com.renzoproject.calc.core.reference.ReferenceJson;

/**
 * Loads {@code reference/common/air-properties.json} from the classpath once. Mirrors
 * {@code JsonFirePumpMotorSizeResolver}'s loading approach: read through {@code ReferenceJson},
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
				file.properties().specificGasConstant().value(),
				file.properties().gravitationalAcceleration().value());
	}

	private static AirPropertiesFile load() {
		return ReferenceJson.read(RESOURCE_PATH, AirPropertiesFile.class);
	}

	@Override
	public AirProperties properties() {
		return properties;
	}

}
