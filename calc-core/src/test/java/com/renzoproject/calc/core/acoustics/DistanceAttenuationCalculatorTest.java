package com.renzoproject.calc.core.acoustics;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistanceAttenuationCalculatorTest {

	private static final double DELTA = 1e-9;

	private final DistanceAttenuationCalculator calculator = new DistanceAttenuationCalculator();

	@Test
	void doublingDistance_dropsSplBySixDbPerDoublingRuleOfThumb() {
		// 94 dB at 1m -> ~87.98 dB at 2m (94 + (-6.020599913279624)).
		DistanceAttenuationInput input = new DistanceAttenuationInput(94.0, 1.0, 2.0);

		DistanceAttenuationResult result = calculator.calculate(input);

		assertEquals(-6.020599913279624, result.attenuationDb(), DELTA);
		assertEquals(87.97940008672037, result.targetSplDb(), DELTA);
	}

	@Test
	void movingCloser_increasesSpl() {
		DistanceAttenuationInput input = new DistanceAttenuationInput(80.0, 4.0, 1.0);

		DistanceAttenuationResult result = calculator.calculate(input);

		assertTrue(result.attenuationDb() > 0);
		assertTrue(result.targetSplDb() > 80.0);
		assertEquals(80.0 + AcousticsMath.distanceAttenuationDb(4.0, 1.0), result.targetSplDb(), DELTA);
	}

	@Test
	void sameDistance_noChange() {
		DistanceAttenuationInput input = new DistanceAttenuationInput(85.0, 3.0, 3.0);

		DistanceAttenuationResult result = calculator.calculate(input);

		assertEquals(0.0, result.attenuationDb(), DELTA);
		assertEquals(85.0, result.targetSplDb(), DELTA);
	}

	@Test
	void nonPositiveReferenceDistance_throws() {
		assertThrows(CalculationException.class, () -> new DistanceAttenuationInput(90.0, 0.0, 2.0));
		assertThrows(CalculationException.class, () -> new DistanceAttenuationInput(90.0, -1.0, 2.0));
	}

	@Test
	void nonPositiveTargetDistance_throws() {
		assertThrows(CalculationException.class, () -> new DistanceAttenuationInput(90.0, 1.0, 0.0));
		assertThrows(CalculationException.class, () -> new DistanceAttenuationInput(90.0, 1.0, -2.0));
	}

}
