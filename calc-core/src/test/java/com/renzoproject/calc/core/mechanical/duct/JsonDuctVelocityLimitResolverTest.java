package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link JsonDuctVelocityLimitResolver} against the real {@code duct-velocity-limits.json}
 * reference data.
 */
class JsonDuctVelocityLimitResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonDuctVelocityLimitResolver resolver = new JsonDuctVelocityLimitResolver();

	@Test
	void resolveMaxVelocity_withinOccupiedSpace_round_matchesPublishedValue() {
		assertEquals(13.2, resolver.resolveMaxVelocity("WITHIN_OCCUPIED_SPACE", 35, DuctShape.ROUND), DELTA);
	}

	@Test
	void resolveMaxVelocity_withinOccupiedSpace_rectangular_matchesPublishedValue() {
		assertEquals(7.4, resolver.resolveMaxVelocity("WITHIN_OCCUPIED_SPACE", 35, DuctShape.RECTANGULAR), DELTA);
	}

	@Test
	void resolveMaxVelocity_inShaft_round_matchesPublishedValue() {
		assertEquals(25.4, resolver.resolveMaxVelocity("IN_SHAFT_OR_ABOVE_SOLID_DRYWALL_CEILING", 45, DuctShape.ROUND), DELTA);
	}

	@Test
	void resolveMaxVelocity_unknownCombination_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolveMaxVelocity("WITHIN_OCCUPIED_SPACE", 999, DuctShape.ROUND));
	}

	@Test
	void resolveMaxVelocity_nullDuctLocation_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolveMaxVelocity(null, 35, DuctShape.ROUND));
	}

}
