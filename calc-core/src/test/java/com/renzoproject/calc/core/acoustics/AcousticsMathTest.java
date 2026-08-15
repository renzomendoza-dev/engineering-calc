package com.renzoproject.calc.core.acoustics;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcousticsMathTest {

	private static final double DELTA = 1e-9;

	@Test
	void distanceAttenuationDb_doublingDistance_matchesSixDbPerDoublingRuleOfThumb() {
		// Field-value sanity check, analogous to the 10HP/230V/28A motor FLC check: doubling
		// distance should produce ~6.02 dB of attenuation (20*log10(2) = 6.020599913279624...).
		double attenuationDb = AcousticsMath.distanceAttenuationDb(1.0, 2.0);

		assertEquals(-6.020599913279624, attenuationDb, DELTA);
		assertEquals(6.02, Math.abs(attenuationDb), 0.01);
	}

	@Test
	void distanceAttenuationDb_movingFarther_isNegative() {
		assertTrue(AcousticsMath.distanceAttenuationDb(1.0, 4.0) < 0);
	}

	@Test
	void distanceAttenuationDb_movingCloser_isPositive() {
		assertTrue(AcousticsMath.distanceAttenuationDb(4.0, 1.0) > 0);
	}

	@Test
	void distanceAttenuationDb_sameDistance_isZero() {
		assertEquals(0.0, AcousticsMath.distanceAttenuationDb(5.0, 5.0), DELTA);
	}

	@Test
	void distanceAttenuationDb_movingFartherThenCloserBackToStart_cancelsOut() {
		double away = AcousticsMath.distanceAttenuationDb(1.0, 3.0);
		double back = AcousticsMath.distanceAttenuationDb(3.0, 1.0);

		assertEquals(0.0, away + back, DELTA);
	}

	@Test
	void distanceAttenuationDb_nonPositiveD1_throws() {
		assertThrows(CalculationException.class, () -> AcousticsMath.distanceAttenuationDb(0.0, 2.0));
		assertThrows(CalculationException.class, () -> AcousticsMath.distanceAttenuationDb(-1.0, 2.0));
	}

	@Test
	void distanceAttenuationDb_nonPositiveD2_throws() {
		assertThrows(CalculationException.class, () -> AcousticsMath.distanceAttenuationDb(1.0, 0.0));
		assertThrows(CalculationException.class, () -> AcousticsMath.distanceAttenuationDb(1.0, -2.0));
	}

}
