package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplyCurveTest {

	private static final double DELTA = 1e-9;

	/** 80 psi static, 60 psi residual at 1000 gpm. */
	private final SupplyCurve curve = new SupplyCurve(80.0, 60.0, 1000.0);

	@Test
	void atZeroFlow_returnsStaticPressure() {
		assertEquals(80.0, curve.availablePressurePsi(0.0), DELTA);
	}

	@Test
	void atTestFlow_returnsResidualPressure() {
		assertEquals(60.0, curve.availablePressurePsi(1000.0), DELTA);
	}

	@Test
	void beyondTestFlow_followsThe1point85Curve() {
		// P(1500) = 80 - (80-60) * (1500/1000)^1.85
		//         = 80 - 20 * 1.5^1.85 = 80 - 20 * 2.11777 = 80 - 42.3554 = 37.655 psi (~37.7)
		assertEquals(37.655, curve.availablePressurePsi(1500.0), 0.001);
	}

	@Test
	void farBeyondTestFlow_canGoNegative() {
		// Not an error: it means the supply cannot deliver that flow, which the calculator
		// reports through a negative safety margin rather than by throwing.
		assertTrue(curve.availablePressurePsi(5000.0) < 0);
	}

	@Test
	void construct_residualNotBelowStatic_throws() {
		assertThrows(CalculationException.class, () -> new SupplyCurve(60.0, 60.0, 1000.0));
		assertThrows(CalculationException.class, () -> new SupplyCurve(60.0, 80.0, 1000.0));
	}

	@Test
	void construct_nonPositiveTestFlow_throws() {
		assertThrows(CalculationException.class, () -> new SupplyCurve(80.0, 60.0, 0.0));
	}

	@Test
	void availablePressure_negativeFlow_throws() {
		assertThrows(CalculationException.class, () -> curve.availablePressurePsi(-1.0));
	}

}
