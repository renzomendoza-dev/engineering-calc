package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link JsonFireWaterDurationResolver} against the real {@code fire-water-duration.json}
 * reference data for all three hazard classifications.
 */
class JsonFireWaterDurationResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonFireWaterDurationResolver resolver = new JsonFireWaterDurationResolver();

	@Test
	void resolve_lightHazard_fixedThirtyMinutes_minEqualsMax() {
		DurationRange range = resolver.resolve(HazardClassification.LIGHT_HAZARD);

		assertEquals(30.0, range.minMinutes(), DELTA);
		assertEquals(30.0, range.maxMinutes(), DELTA);
		assertEquals(100.0, range.hoseStreamAllowanceGpm(), DELTA);
	}

	@Test
	void resolve_ordinaryHazard_sixtyToNinetyMinutes() {
		DurationRange range = resolver.resolve(HazardClassification.ORDINARY_HAZARD);

		assertEquals(60.0, range.minMinutes(), DELTA);
		assertEquals(90.0, range.maxMinutes(), DELTA);
		assertEquals(250.0, range.hoseStreamAllowanceGpm(), DELTA);
	}

	@Test
	void resolve_extraHazard_ninetyToOneTwentyMinutes() {
		DurationRange range = resolver.resolve(HazardClassification.EXTRA_HAZARD);

		assertEquals(90.0, range.minMinutes(), DELTA);
		assertEquals(120.0, range.maxMinutes(), DELTA);
		assertEquals(500.0, range.hoseStreamAllowanceGpm(), DELTA);
	}

	@Test
	void resolve_null_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolve(null));
	}

}
