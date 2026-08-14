package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LockedRotorSinglePhaseTableTest {

	private static final double DELTA = 1e-9;

	private final LockedRotorSinglePhaseTable table = new LockedRotorSinglePhaseTable();

	@Test
	void lookup_5Hp230V_matchesPublishedValue() {
		assertEquals(168.0, table.lookup("5", 230), DELTA);
	}

	@Test
	void lookup_unknownCombination_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> table.lookup("9999", 230));
	}

}
