package com.renzoproject.calc.core.mechanical.sprinkler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElevationPressureTest {

	private static final double DELTA = 1e-9;

	@Test
	void tenFootRise_costs4point33Psi() {
		// 0.433 psi/ft * 10 ft = 4.33 psi
		assertEquals(4.33, ElevationPressure.psiForRise(10.0), DELTA);
	}

	@Test
	void drop_givesPressureBack() {
		// Signed: a drop returns a negative value, which reduces the required pressure.
		assertEquals(-4.33, ElevationPressure.psiForRise(-10.0), DELTA);
	}

	@Test
	void levelRun_isZero() {
		assertEquals(0.0, ElevationPressure.psiForRise(0.0), DELTA);
	}

}
