package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AmbientTempCorrectionTableTest {

	private static final double DELTA = 1e-9;

	private final AmbientTempCorrectionTable table = new AmbientTempCorrectionTable();

	@Test
	void lookup_35CAmbientFor75CConductor_matchesPublishedFactor() {
		assertEquals(0.94, table.lookup(35, 75), DELTA);
	}

	@Test
	void lookup_baselineRange_factorIsOne() {
		assertEquals(1.0, table.lookup(28, 90), DELTA);
	}

	@Test
	void lookup_tenOrLessRow_treatsAnythingAtOrBelow10AsMatching() {
		assertEquals(1.15, table.lookup(10, 90), DELTA);
		assertEquals(1.15, table.lookup(0, 90), DELTA);
	}

	@Test
	void lookup_60CConductorAt65CAmbient_throwsCalculationException() {
		// row 61-65 has factor60C = null — a 60C conductor cannot run at 65C ambient without
		// other correction, per the source table.
		assertThrows(CalculationException.class, () -> table.lookup(65, 60));
	}

	@Test
	void lookup_ambientAboveHighestRange_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> table.lookup(90, 90));
	}

}
