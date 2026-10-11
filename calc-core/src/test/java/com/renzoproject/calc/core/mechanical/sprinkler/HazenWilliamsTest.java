package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HazenWilliamsTest {

	@Test
	void frictionLoss_2inSch40_100gpm_matchesHandCalculation() {
		// p = 4.52 * Q^1.85 / (C^1.85 * d^4.87), Q=100 gpm, C=120, d=2.067 in (2" Sch 40):
		//   Q^1.85 = 5011.87, C^1.85 = 7021.7, d^4.87 = 34.36
		//   p = 4.52 * 5011.87 / (7021.7 * 34.36) = 22653.6 / 241,265 = 0.09396 psi/ft
		// Cross-checked independently before asserting; the commonly quoted figure is ~0.094.
		assertEquals(0.09396, HazenWilliams.frictionLossPsiPerFoot(100.0, 120.0, 2.067), 0.00001);
	}

	@Test
	void frictionLoss_risesSharplyWithFlow() {
		// 1.85 exponent: doubling flow multiplies loss by 2^1.85 = 3.606.
		double atFifty = HazenWilliams.frictionLossPsiPerFoot(50.0, 120.0, 2.067);
		double atHundred = HazenWilliams.frictionLossPsiPerFoot(100.0, 120.0, 2.067);

		assertEquals(Math.pow(2.0, 1.85), atHundred / atFifty, 1e-9);
	}

	@Test
	void frictionLoss_lowerCFactorCostsMorePressure() {
		// A dry system (C=100) loses more than a wet one (C=120) at the same flow and size.
		double wet = HazenWilliams.frictionLossPsiPerFoot(100.0, 120.0, 2.067);
		double dry = HazenWilliams.frictionLossPsiPerFoot(100.0, 100.0, 2.067);

		assertTrue(dry > wet, "expected C=100 to lose more than C=120, was " + dry + " vs " + wet);
	}

	@Test
	void frictionLoss_zeroFlow_isZero() {
		assertEquals(0.0, HazenWilliams.frictionLossPsiPerFoot(0.0, 120.0, 2.067), 0.0);
	}

	@Test
	void frictionLoss_negativeFlow_throws() {
		assertThrows(CalculationException.class, () -> HazenWilliams.frictionLossPsiPerFoot(-1.0, 120.0, 2.067));
	}

	@Test
	void frictionLoss_nonPositiveCFactor_throws() {
		assertThrows(CalculationException.class, () -> HazenWilliams.frictionLossPsiPerFoot(100.0, 0.0, 2.067));
	}

	@Test
	void frictionLoss_nonPositiveDiameter_throws() {
		assertThrows(CalculationException.class, () -> HazenWilliams.frictionLossPsiPerFoot(100.0, 120.0, 0.0));
	}

	@Test
	void equivalentLengthAdjustment_reproducesNfpaPublishedFactors() {
		// NFPA 13 publishes rounded multipliers for converting its C=120 equivalent length chart
		// to other C values. (C/120)^1.85 reproduces them; (120/C)^1.85 does not, which is the
		// easy way to get this backwards.
		assertEquals(0.713, HazenWilliams.equivalentLengthAdjustmentFactor(100.0, 120.0), 0.001);
		assertEquals(1.16, HazenWilliams.equivalentLengthAdjustmentFactor(130.0, 120.0), 0.001);
		assertEquals(1.33, HazenWilliams.equivalentLengthAdjustmentFactor(140.0, 120.0), 0.001);
		assertEquals(1.51, HazenWilliams.equivalentLengthAdjustmentFactor(150.0, 120.0), 0.002);
	}

	@Test
	void equivalentLengthAdjustment_atTheBasis_isExactlyOne() {
		assertEquals(1.0, HazenWilliams.equivalentLengthAdjustmentFactor(120.0, 120.0), 0.0);
	}

	@Test
	void equivalentLengthAdjustment_rougherPipeNeedsFewerEquivalentFeet() {
		// Lower C means more loss per foot, so fewer feet of it match the fitting's own loss.
		assertTrue(HazenWilliams.equivalentLengthAdjustmentFactor(100.0, 120.0) < 1.0);
		assertTrue(HazenWilliams.equivalentLengthAdjustmentFactor(150.0, 120.0) > 1.0);
	}

	@Test
	void equivalentLengthAdjustment_nonPositiveArguments_throw() {
		assertThrows(CalculationException.class, () -> HazenWilliams.equivalentLengthAdjustmentFactor(0.0, 120.0));
		assertThrows(CalculationException.class, () -> HazenWilliams.equivalentLengthAdjustmentFactor(120.0, 0.0));
	}

}
