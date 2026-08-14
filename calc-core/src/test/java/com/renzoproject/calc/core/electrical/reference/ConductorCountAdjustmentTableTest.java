package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConductorCountAdjustmentTableTest {

	private static final double DELTA = 1e-9;

	private final ConductorCountAdjustmentTable table = new ConductorCountAdjustmentTable();

	@Test
	void lookup_oneToThreeConductors_noReduction() {
		assertEquals(1.0, table.lookup(1), DELTA);
		assertEquals(1.0, table.lookup(3), DELTA);
	}

	@Test
	void lookup_eightConductors_returns70Percent() {
		assertEquals(0.70, table.lookup(8), DELTA);
	}

	@Test
	void lookup_fortyOneAndAbove_isOpenEnded() {
		assertEquals(0.35, table.lookup(41), DELTA);
		assertEquals(0.35, table.lookup(200), DELTA);
	}

	@Test
	void lookup_countLessThanOne_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> table.lookup(0));
	}

}
