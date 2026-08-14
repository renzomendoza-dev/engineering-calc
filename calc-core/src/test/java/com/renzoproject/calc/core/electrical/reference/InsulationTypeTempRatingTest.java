package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsulationTypeTempRatingTest {

	private final InsulationTypeTempRating table = new InsulationTypeTempRating();

	@Test
	void lookup_thhnCopper_mapsTo90C() {
		assertEquals(90, table.lookup(InsulationType.THHN, ConductorMaterial.COPPER));
	}

	@Test
	void lookup_twCopper_mapsTo60C() {
		assertEquals(60, table.lookup(InsulationType.TW, ConductorMaterial.COPPER));
	}

	@Test
	void lookup_rhwCopper_mapsTo75C() {
		assertEquals(75, table.lookup(InsulationType.RHW, ConductorMaterial.COPPER));
	}

	@Test
	void lookup_zwAluminum_throwsCalculationException() {
		// ZW at 75C is published for copper only, not aluminum.
		assertThrows(CalculationException.class, () -> table.lookup(InsulationType.ZW, ConductorMaterial.ALUMINUM));
	}

}
