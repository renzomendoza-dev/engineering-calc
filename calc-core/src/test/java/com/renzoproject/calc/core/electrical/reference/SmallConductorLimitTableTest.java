package com.renzoproject.calc.core.electrical.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** PEC 2017 2.40.1.4(D) Small Conductors, as printed on page 84. */
class SmallConductorLimitTableTest {

	private final SmallConductorLimitTable table = new SmallConductorLimitTable();

	private int limit(ConductorMaterial material, String size) {
		return table.lookup(material, size).orElseThrow().maximumOvercurrentDeviceAmps();
	}

	@Test
	void theSevenPrintedLimits_areAsPrinted() {
		assertEquals(7, limit(ConductorMaterial.COPPER, "0.75"));
		assertEquals(10, limit(ConductorMaterial.COPPER, "1.25"));
		assertEquals(15, limit(ConductorMaterial.COPPER, "2.0"));
		assertEquals(15, limit(ConductorMaterial.ALUMINUM, "3.5"));
		assertEquals(20, limit(ConductorMaterial.COPPER, "3.5"));
		assertEquals(25, limit(ConductorMaterial.ALUMINUM, "5.5"));
		assertEquals(30, limit(ConductorMaterial.COPPER, "5.5"));
		assertEquals(7, table.allEntries().size());
	}

	@Test
	void aSizeTheClauseDoesNotCover_hasNoLimit() {
		assertTrue(table.lookup(ConductorMaterial.COPPER, "8.0").isEmpty());
		assertTrue(table.lookup(ConductorMaterial.ALUMINUM, "8.0").isEmpty());
		assertTrue(table.lookup(ConductorMaterial.COPPER, "14").isEmpty());
		// There is no printed limit for 2.0 mm2 aluminum: it is not an available size (Table 3.10.2.6(B)(16)).
		assertTrue(table.lookup(ConductorMaterial.ALUMINUM, "2.0").isEmpty());
	}

	@Test
	void onlyTheTwoSmallestCopperSizesCarryConditions() {
		assertTrue(table.lookup(ConductorMaterial.COPPER, "0.75").orElseThrow().conditions().contains("5.6 A"));
		assertTrue(table.lookup(ConductorMaterial.COPPER, "1.25").orElseThrow().conditions().contains("8 A"));
		for (SmallConductorLimitEntry entry : List.of(
				table.lookup(ConductorMaterial.COPPER, "2.0").orElseThrow(),
				table.lookup(ConductorMaterial.ALUMINUM, "3.5").orElseThrow(),
				table.lookup(ConductorMaterial.COPPER, "3.5").orElseThrow(),
				table.lookup(ConductorMaterial.ALUMINUM, "5.5").orElseThrow(),
				table.lookup(ConductorMaterial.COPPER, "5.5").orElseThrow())) {
			assertNull(entry.conditions());
		}
	}

	@Test
	void theTableSaysWhereItWasRead() {
		assertTrue(table.standard().contains("2.40.1.4(D)"));
		assertTrue(table.note().contains("copper-clad aluminum"));
	}

}
