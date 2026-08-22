package com.renzoproject.calc.core.mechanical;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.function.DoubleUnaryOperator;

/**
 * Generic bisection root-finder, shared by both of {@code mechanical.duct}'s
 * {@code DuctSizingCalculator} iterative solves (round Equal Friction diameter, rectangular
 * free-dimension solving) rather than writing a bespoke iteration loop per call site.
 *
 * <p>Bisection over Newton-Raphson: both call sites solve a single-unknown, monotonic function
 * (friction rate decreases monotonically as diameter increases for a fixed flow; ASHRAE's
 * equivalent-diameter formula increases monotonically in the free dimension for a fixed opposite
 * dimension), so bisection's only real requirement -- a bracketing interval where the function
 * changes sign -- is easy to construct for both, and it needs no derivative (the friction-rate
 * function isn't cleanly differentiable in closed form once Colebrook-White's own inner iteration
 * is involved).
 */
public final class BisectionSolver {

	private static final int MAX_ITERATIONS = 100;

	private BisectionSolver() {
	}

	/**
	 * @param f                 function whose root is sought
	 * @param lowerBound        lower bracket; {@code f(lowerBound)} must have the opposite sign
	 *                          (or be exactly zero) from {@code f(upperBound)}
	 * @param upperBound        upper bracket
	 * @param relativeTolerance convergence tolerance on the bracket width relative to the current
	 *                          midpoint's magnitude
	 * @throws CalculationException if {@code f(lowerBound)} and {@code f(upperBound)} don't
	 *                               bracket a root (same sign, both nonzero), or if convergence
	 *                               isn't reached within {@value #MAX_ITERATIONS} iterations
	 */
	public static double solve(DoubleUnaryOperator f, double lowerBound, double upperBound, double relativeTolerance) {
		double flo = f.applyAsDouble(lowerBound);
		if (flo == 0.0) {
			return lowerBound;
		}
		double fhi = f.applyAsDouble(upperBound);
		if (fhi == 0.0) {
			return upperBound;
		}
		if (Math.signum(flo) == Math.signum(fhi)) {
			throw new CalculationException("Bisection failed: f(lowerBound=" + lowerBound + ")=" + flo + " and "
					+ "f(upperBound=" + upperBound + ")=" + fhi + " do not bracket a root (same sign) -- cannot converge");
		}

		double lo = lowerBound;
		double hi = upperBound;
		for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
			double mid = (lo + hi) / 2.0;
			double fmid = f.applyAsDouble(mid);
			if (fmid == 0.0 || (hi - lo) / Math.abs(mid) < relativeTolerance) {
				return mid;
			}
			if (Math.signum(fmid) == Math.signum(flo)) {
				lo = mid;
				flo = fmid;
			} else {
				hi = mid;
			}
		}
		throw new CalculationException("Bisection did not converge within " + MAX_ITERATIONS
				+ " iterations (relative tolerance " + relativeTolerance + ")");
	}

}
