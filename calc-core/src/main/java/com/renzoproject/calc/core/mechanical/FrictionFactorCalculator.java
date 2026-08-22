package com.renzoproject.calc.core.mechanical;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;

/**
 * Colebrook-White / Swamee-Jain Darcy friction factor for turbulent/transitional flow, extracted
 * from {@code mechanical.pipe.PipePressureLossCalculator} so {@code mechanical.duct}'s
 * {@code DuctSizingCalculator} can reuse the identical correlations rather than reimplementing
 * them. Pure extraction -- same formulas, same convergence behavior, same public numbers;
 * {@code PipePressureLossCalculatorTest}'s existing regression values are unchanged by this
 * refactor.
 *
 * <p>Laminar flow ({@code f = 64/Re}) is deliberately NOT handled here -- that was never part of
 * the extracted method in {@code PipePressureLossCalculator} either (it branches on flow regime
 * before calling in), so both callers keep owning that trivial branch themselves.
 *
 * <p>Operates in consistent SI base units throughout. Reynolds number and relative roughness are
 * both dimensionless ratios, so there's no meters-vs-millimeters ambiguity inside this utility
 * itself -- callers are responsible for converting their own mm-facing inputs (e.g. ASHRAE's duct
 * equations, which use hydraulic diameter in millimeters with unit-conversion constants baked
 * into the formula) into a dimensionless relative roughness before calling in. Mixing the two
 * unit conventions inside this shared utility would be a correctness bug waiting to happen.
 */
public final class FrictionFactorCalculator {

	private static final double CONVERGENCE_RELATIVE_TOLERANCE = 1e-6;
	private static final int MAX_COLEBROOK_ITERATIONS = 50;

	private FrictionFactorCalculator() {
	}

	/**
	 * Swamee-Jain is always computed first (it's explicit, and doubles as the Colebrook-White
	 * seed value when that method is requested), then refined via Colebrook-White if that's what
	 * {@code method} asked for.
	 *
	 * @param reynoldsNumber    must be positive; turbulent/transitional only (see class Javadoc)
	 * @param relativeRoughness roughness / hydraulic diameter, dimensionless
	 * @throws CalculationException if Colebrook-White iteration doesn't converge within
	 *                               {@value #MAX_COLEBROOK_ITERATIONS} iterations
	 */
	public static double frictionFactor(double reynoldsNumber, double relativeRoughness, FrictionFactorMethod method) {
		double swameeJain = swameeJainFrictionFactor(relativeRoughness, reynoldsNumber);
		return method == FrictionFactorMethod.SWAMEE_JAIN
				? swameeJain
				: colebrookWhiteFrictionFactor(relativeRoughness, reynoldsNumber, swameeJain);
	}

	private static double swameeJainFrictionFactor(double relativeRoughness, double reynoldsNumber) {
		double term = relativeRoughness / 3.7 + 5.74 / Math.pow(reynoldsNumber, 0.9);
		double log10Term = Math.log10(term);
		return 0.25 / (log10Term * log10Term);
	}

	/**
	 * Standard fixed-point iteration on {@code x = 1/sqrt(f)}:
	 * {@code x_next = -2*log10(relativeRoughness/3.7 + 2.51*x/Re)} — converges reliably for this
	 * equation's shape (the textbook approach), unlike iterating on {@code f} directly.
	 */
	private static double colebrookWhiteFrictionFactor(double relativeRoughness, double reynoldsNumber, double initialGuess) {
		double x = 1.0 / Math.sqrt(initialGuess);
		for (int iteration = 0; iteration < MAX_COLEBROOK_ITERATIONS; iteration++) {
			double xNext = -2.0 * Math.log10(relativeRoughness / 3.7 + 2.51 * x / reynoldsNumber);
			double relativeChange = Math.abs(xNext - x) / Math.abs(x);
			x = xNext;
			if (relativeChange < CONVERGENCE_RELATIVE_TOLERANCE) {
				return 1.0 / (x * x);
			}
		}
		throw new CalculationException("Colebrook-White iteration did not converge within " + MAX_COLEBROOK_ITERATIONS
				+ " iterations (relative tolerance " + CONVERGENCE_RELATIVE_TOLERANCE + ")");
	}

}
