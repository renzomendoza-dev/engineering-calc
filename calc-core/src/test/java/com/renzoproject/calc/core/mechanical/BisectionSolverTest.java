package com.renzoproject.calc.core.mechanical;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BisectionSolverTest {

	private static final double DELTA = 1e-6;

	@Test
	void solve_linearFunction_findsExactRoot() {
		// f(x) = x - 3, root at x=3.
		double root = BisectionSolver.solve(x -> x - 3.0, 0.0, 10.0, 1e-9);

		assertEquals(3.0, root, DELTA);
	}

	@Test
	void solve_nonlinearFunction_convergesWithinTolerance() {
		// f(x) = x^2 - 2, root at x=sqrt(2).
		double root = BisectionSolver.solve(x -> x * x - 2.0, 0.0, 2.0, 1e-9);

		assertEquals(Math.sqrt(2.0), root, DELTA);
	}

	@Test
	void solve_boundsDoNotBracketRoot_throws() {
		// f(x) = x - 3 is positive at both bounds -- no sign change.
		assertThrows(CalculationException.class, () -> BisectionSolver.solve(x -> x - 3.0, 5.0, 10.0, 1e-9));
	}

	@Test
	void solve_rootExactlyAtLowerBound_returnsItDirectly() {
		double root = BisectionSolver.solve(x -> x - 5.0, 5.0, 10.0, 1e-9);

		assertEquals(5.0, root, DELTA);
	}

}
