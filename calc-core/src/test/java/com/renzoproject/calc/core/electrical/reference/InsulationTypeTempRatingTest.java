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
	void lookup_thhwAndXhhw_aretheDualRatedTypes_heldTo75C_forBothMaterials() {
		// THHW (75 C wet, 90 C dry) and XHHW take the lower rating: a lookup gives one, and 75 C is right in a wet location.
		assertEquals(75, table.lookup(InsulationType.THHW, ConductorMaterial.COPPER));
		assertEquals(75, table.lookup(InsulationType.THHW, ConductorMaterial.ALUMINUM));
		assertEquals(75, table.lookup(InsulationType.XHHW, ConductorMaterial.COPPER));
		assertEquals(75, table.lookup(InsulationType.XHHW, ConductorMaterial.ALUMINUM));
	}

	@Test
	void lookup_theNinetyDegreeTypesAroundThem_areUnchanged() {
		assertEquals(90, table.lookup(InsulationType.THW_2, ConductorMaterial.COPPER));
		assertEquals(90, table.lookup(InsulationType.XHHW_2, ConductorMaterial.COPPER));
		assertEquals(90, table.lookup(InsulationType.XHH, ConductorMaterial.ALUMINUM));
		assertEquals(75, table.lookup(InsulationType.THW, ConductorMaterial.COPPER));
	}

	@Test
	void allEntries_haveOneRatingPerTypeAndMaterial() {
		var seen = new java.util.HashSet<String>();
		for (var entry : table.allEntries()) {
			assertEquals(true, seen.add(entry.insulationType() + "|" + entry.conductorMaterial()),
					"two ratings for " + entry.insulationType() + " " + entry.conductorMaterial());
		}
	}

	@Test
	void lookup_zwAluminum_throwsCalculationException() {
		// ZW at 75C is published for copper only, not aluminum.
		assertThrows(CalculationException.class, () -> table.lookup(InsulationType.ZW, ConductorMaterial.ALUMINUM));
	}

}
