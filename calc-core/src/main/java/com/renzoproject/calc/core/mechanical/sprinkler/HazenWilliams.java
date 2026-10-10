package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Hazen-Williams friction loss for sprinkler piping, NFPA 13's required method:
 * {@code p = 4.52 * Q^1.85 / (C^1.85 * d^4.87)}.
 *
 * <p><b>Units:</b> {@code p} is psi per foot of pipe, {@code Q} is US gpm, {@code d} is actual
 * inside diameter in inches, {@code C} is dimensionless. The 4.52 coefficient is specific to that
 * unit set -- this formula is not unit-agnostic, so callers must convert before calling. Kept as
 * plain doubles for exactly that reason, matching how every other calculator in this codebase does
 * its internal arithmetic.
 *
 * <p>Deliberately separate from {@code mechanical.FrictionFactorCalculator} /
 * {@code PipePressureLossCalculator}, which implement Darcy-Weisbach with Colebrook-White or
 * Swamee-Jain friction factors. Both are correct for their own domain; NFPA 13 mandates
 * Hazen-Williams for sprinkler hydraulic calculations, so the two coexist rather than one
 * delegating to the other.
 */
public final class HazenWilliams {

	private static final double COEFFICIENT = 4.52;
	private static final double FLOW_EXPONENT = 1.85;
	private static final double C_EXPONENT = 1.85;
	private static final double DIAMETER_EXPONENT = 4.87;

	private HazenWilliams() {
	}

	/**
	 * @param flowGpm            flow through the pipe, US gpm; must not be negative
	 * @param cFactor            Hazen-Williams C; must be positive
	 * @param insideDiameterInch actual inside diameter in inches (never the nominal size); must be
	 *                           positive
	 * @return friction loss in psi per foot of pipe
	 * @throws CalculationException if any argument is out of range
	 */
	public static double frictionLossPsiPerFoot(double flowGpm, double cFactor, double insideDiameterInch) {
		if (flowGpm < 0) {
			throw new CalculationException("flowGpm must not be negative (was " + flowGpm + ")");
		}
		if (cFactor <= 0) {
			throw new CalculationException("cFactor must be positive (was " + cFactor + ")");
		}
		if (insideDiameterInch <= 0) {
			throw new CalculationException("insideDiameterInch must be positive (was " + insideDiameterInch + ")");
		}

		return COEFFICIENT * Math.pow(flowGpm, FLOW_EXPONENT)
				/ (Math.pow(cFactor, C_EXPONENT) * Math.pow(insideDiameterInch, DIAMETER_EXPONENT));
	}

}
