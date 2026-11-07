package com.renzoproject.calc.core.mechanical;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Hazen-Williams friction loss for fire protection piping, the method NFPA 13 (sprinklers) and
 * NFPA 14 (standpipes, 11.3) both require: {@code p = 4.52 * Q^1.85 / (C^1.85 * d^4.87)}, with the
 * rescaling of published fitting equivalent lengths for another C or inside diameter.
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

	/**
	 * Multiplier that converts a published fitting equivalent length to the pipe actually being
	 * used: {@code (C / publishedBasisC)^1.85}.
	 *
	 * <p>NFPA 13's equivalent length chart is expressed in feet of C=120 pipe, and the standard
	 * requires rescaling those lengths for pipe with a different C. The direction is easy to get
	 * backwards: a fitting's own loss barely depends on the pipe it sits in, so in rougher pipe
	 * (lower C, more loss per foot) it takes <i>fewer</i> feet of that pipe to match the fitting --
	 * the factor is below 1 for C below the basis. This formula reproduces NFPA's published
	 * factors: 0.713 at C=100, 1.16 at C=130, 1.33 at C=140, 1.51 at C=150.
	 *
	 * @param cFactor             the segment's actual Hazen-Williams C; must be positive
	 * @param publishedBasisC     the C the published lengths assume (120 for NFPA 13's chart);
	 *                            must be positive
	 * @throws CalculationException if either argument is out of range
	 */
	public static double equivalentLengthAdjustmentFactor(double cFactor, double publishedBasisC) {
		if (cFactor <= 0) {
			throw new CalculationException("cFactor must be positive (was " + cFactor + ")");
		}
		if (publishedBasisC <= 0) {
			throw new CalculationException("publishedBasisC must be positive (was " + publishedBasisC + ")");
		}
		return Math.pow(cFactor / publishedBasisC, C_EXPONENT);
	}

	/**
	 * Multiplier that converts a published fitting equivalent length to a pipe whose inside diameter
	 * differs from the one the chart assumes: {@code (actual ID / basis ID)^4.87} (NFPA 14 11.3.2.5,
	 * where the basis is Schedule 40 steel).
	 *
	 * @param insideDiameterInch      the segment's actual inside diameter, in; must be positive
	 * @param basisInsideDiameterInch inside diameter of the pipe the published lengths assume, in; must be positive
	 * @throws CalculationException if either argument is out of range
	 */
	public static double diameterAdjustmentFactor(double insideDiameterInch, double basisInsideDiameterInch) {
		if (insideDiameterInch <= 0) {
			throw new CalculationException("insideDiameterInch must be positive (was " + insideDiameterInch + ")");
		}
		if (basisInsideDiameterInch <= 0) {
			throw new CalculationException("basisInsideDiameterInch must be positive (was " + basisInsideDiameterInch + ")");
		}
		return Math.pow(insideDiameterInch / basisInsideDiameterInch, DIAMETER_EXPONENT);
	}

}
