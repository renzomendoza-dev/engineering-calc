package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link JsonFirePumpMotorSizeResolver} against the real {@code motor-hp-steps.json}
 * reference data.
 */
class JsonFirePumpMotorSizeResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonFirePumpMotorSizeResolver resolver = new JsonFirePumpMotorSizeResolver();

	@Test
	void resolveNextStandardMotorHp_roundsUpToPublishedStep() {
		// Published steps include ...30, 40...
		assertEquals(40.0, resolver.resolveNextStandardMotorHp(38.0), DELTA);
	}

	@Test
	void resolveNextStandardMotorHp_exceedsLargestStep_throwsRatherThanClamping() {
		// Largest published step is 1000 HP.
		assertThrows(CalculationException.class, () -> resolver.resolveNextStandardMotorHp(1001.0));
	}

}
