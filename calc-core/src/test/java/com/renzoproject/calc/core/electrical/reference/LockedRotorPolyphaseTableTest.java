package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LockedRotorPolyphaseTableTest {

	private static final double DELTA = 1e-9;

	private final LockedRotorPolyphaseTable table = new LockedRotorPolyphaseTable();

	@Test
	void lookup_5Hp230V_matchesPublishedValue() {
		assertEquals(92.0, table.lookup("5", 230), DELTA);
	}

	@Test
	void lookup_unknownCombination_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> table.lookup("9999", 230));
	}

}
