package com.renzoproject.calc.core.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link JsonAirPropertiesResolver} against the real {@code air-properties.json} reference
 * data.
 */
class JsonAirPropertiesResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonAirPropertiesResolver resolver = new JsonAirPropertiesResolver();

	@Test
	void properties_matchesPublishedValues() {
		AirProperties properties = resolver.properties();

		assertEquals(1.0, properties.specificHeatKjPerKgK(), DELTA);
		assertEquals(101325.0, properties.atmosphericPressurePa(), DELTA);
		assertEquals(287.0, properties.specificGasConstantJPerKgK(), DELTA);
		assertEquals(9.81, properties.gravitationalAccelerationMPerS2(), DELTA);
	}

}
