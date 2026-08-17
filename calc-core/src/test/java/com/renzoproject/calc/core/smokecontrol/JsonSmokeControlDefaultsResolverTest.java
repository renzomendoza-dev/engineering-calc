package com.renzoproject.calc.core.smokecontrol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link JsonSmokeControlDefaultsResolver} against the real {@code defaults.json}
 * reference data.
 */
class JsonSmokeControlDefaultsResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonSmokeControlDefaultsResolver resolver = new JsonSmokeControlDefaultsResolver();

	@Test
	void defaults_matchesPublishedValues() {
		SmokeControlDefaults defaults = resolver.defaults();

		assertEquals(1.0, defaults.fractionConvectiveHeatInSmokeLayer(), DELTA);
		assertEquals(0.7, defaults.convectiveFraction(), DELTA);
		assertEquals(0.6, defaults.ventDischargeCoefficient(), DELTA);
	}

}
