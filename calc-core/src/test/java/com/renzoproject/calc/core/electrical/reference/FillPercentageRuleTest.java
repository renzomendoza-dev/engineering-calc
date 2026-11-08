package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FillPercentageRuleTest {

	private static final double DELTA = 1e-9;

	@Test
	void oneConductor_returns53Percent() {
		assertEquals(53.0, FillPercentageRule.allowedFillPercent(1), DELTA);
	}

	@Test
	void twoConductors_returns31Percent() {
		assertEquals(31.0, FillPercentageRule.allowedFillPercent(2), DELTA);
	}

	@Test
	void threeOrMoreConductors_returns40Percent() {
		assertEquals(40.0, FillPercentageRule.allowedFillPercent(3), DELTA);
		assertEquals(40.0, FillPercentageRule.allowedFillPercent(10), DELTA);
	}

	@Test
	void nipple_is60Percent_whateverTheConductorCount() {
		// PEC Chapter 10 note (4): a nipple of not more than 600 mm may be filled to 60 percent of its area.
		assertEquals(60.0, FillPercentageRule.allowedFillPercent(1, true), DELTA);
		assertEquals(60.0, FillPercentageRule.allowedFillPercent(2, true), DELTA);
		assertEquals(60.0, FillPercentageRule.allowedFillPercent(3, true), DELTA);
		assertEquals(60.0, FillPercentageRule.allowedFillPercent(12, true), DELTA);
	}

	@Test
	void notANipple_followsTable10_1_1_1() {
		assertEquals(53.0, FillPercentageRule.allowedFillPercent(1, false), DELTA);
		assertEquals(31.0, FillPercentageRule.allowedFillPercent(2, false), DELTA);
		assertEquals(40.0, FillPercentageRule.allowedFillPercent(5, false), DELTA);
	}

	@Test
	void countLessThanOne_throwsCalculationException_forANippleToo() {
		assertThrows(CalculationException.class, () -> FillPercentageRule.allowedFillPercent(0, true));
	}

	@Test
	void countLessThanOne_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> FillPercentageRule.allowedFillPercent(0));
		assertThrows(CalculationException.class, () -> FillPercentageRule.allowedFillPercent(-1));
	}

}
