package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SprinklerMathTest {

	private static final double DELTA = 1e-9;

	@Test
	void flow_k5point6_at7psi_matchesHandCalculation() {
		// Q = K * sqrt(P) = 5.6 * sqrt(7) = 5.6 * 2.6457513 = 14.8162 gpm (~14.8).
		assertEquals(14.8162, SprinklerMath.flowGpm(5.6, 7.0), 0.0001);
	}

	@Test
	void pressure_isTheInverseOfFlow() {
		// P = (Q/K)^2 must undo Q = K*sqrt(P) exactly.
		double pressure = SprinklerMath.pressurePsi(5.6, 14.8162073);
		assertEquals(7.0, pressure, 1e-7);
		assertEquals(14.8162073, SprinklerMath.flowGpm(5.6, pressure), 1e-6);
	}

	@Test
	void pressure_forMinimumRequiredFlow_matchesHandCalculation() {
		// 0.10 gpm/ft2 over 130 ft2 = 13 gpm from a K=5.6 head:
		// P = (13/5.6)^2 = 2.3214286^2 = 5.38903 psi
		assertEquals(5.38903, SprinklerMath.pressurePsi(5.6, 13.0), 0.00001);
	}

	@Test
	void flow_atZeroPressure_isZero() {
		assertEquals(0.0, SprinklerMath.flowGpm(5.6, 0.0), DELTA);
	}

	@Test
	void flow_nonPositiveKFactor_throws() {
		assertThrows(CalculationException.class, () -> SprinklerMath.flowGpm(0.0, 7.0));
	}

	@Test
	void flow_negativePressure_throws() {
		assertThrows(CalculationException.class, () -> SprinklerMath.flowGpm(5.6, -1.0));
	}

	@Test
	void pressure_nonPositiveKFactor_throws() {
		assertThrows(CalculationException.class, () -> SprinklerMath.pressurePsi(-5.6, 13.0));
	}

	@Test
	void pressure_negativeFlow_throws() {
		assertThrows(CalculationException.class, () -> SprinklerMath.pressurePsi(5.6, -13.0));
	}

}
