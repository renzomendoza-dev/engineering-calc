package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SynchronousPowerFactorAdjustmentTableTest {

	private static final double DELTA = 1e-9;

	private final SynchronousPowerFactorAdjustmentTable table = new SynchronousPowerFactorAdjustmentTable();

	@Test
	void lookup_100Percent_returnsUnityMultiplier() {
		assertEquals(1.0, table.lookup(100), DELTA);
	}

	@Test
	void lookup_90Percent_returns1point1Multiplier() {
		assertEquals(1.1, table.lookup(90), DELTA);
	}

	@Test
	void lookup_80Percent_returns1point25Multiplier() {
		assertEquals(1.25, table.lookup(80), DELTA);
	}

	@Test
	void lookup_unsupportedPercent_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> table.lookup(75));
	}

}
