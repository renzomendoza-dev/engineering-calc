package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConduitDimensionTableTest {

	private static final double DELTA = 1e-9;

	private final ConduitDimensionTable table = new ConduitDimensionTable();

	@Test
	void lookup_emtRaceway25mm_matchesPublishedValues() {
		ConduitDimensionEntry entry = table.lookup(ConduitType.EMT, 25);

		assertEquals(295, entry.oneWire53PercentMm2(), DELTA);
		assertEquals(172, entry.twoWires53PercentMm2(), DELTA);
		assertEquals(222, entry.over2Wires40PercentMm2(), DELTA);
		assertEquals(556, entry.totalArea100PercentMm2(), DELTA);
	}

	/** Rows of PEC 2017 Table 10.1.1.4 (printed pp. 933-935) that earlier copies had wrong: 40 %, 60 %, 1 wire, 2 wires, ID, total. */
	@Test
	void lookup_rowsCorrectedAgainstThePrintedTable_matchThePrintedValues() {
		assertRow(ConduitType.IMC, 15, 89, 133, 117, 69, 16.8, 222);
		assertRow(ConduitType.IMC, 20, 151, 226, 200, 117, 21.9, 377);
		assertRow(ConduitType.IMC, 25, 248, 372, 329, 192, 28.1, 620);
		assertRow(ConduitType.LFMC, 25, 226, 338, 299, 175, 26.8, 564);
		assertRow(ConduitType.LFMC, 100, 3275, 4912, 4339, 2538, 102.1, 8187);
		assertRow(ConduitType.FMC, 65, 1267, 1900, 1678, 982, 63.5, 3167);
		assertRow(ConduitType.LFNC_B, 50, 836, 1255, 1108, 648, 51.6, 2091);
		assertRow(ConduitType.LFNC_C, 32, 380.4, 570.6, 504.1, 294.8, 34.8, 951.039);
		assertRow(ConduitType.PVC_SCHEDULE_80, 25, 178, 267, 236, 138, 23.8, 445);
		assertRow(ConduitType.PVC_TYPE_EB, 90, 2932, 4397, 3884, 2272, 96.6, 7329);
	}

	@Test
	void lookup_lfmcHasNo130mmSize_andImcHasNo10mmSize() {
		// LFMC is printed 10 to 100 mm and IMC 15 to 100 mm (the printed IMC 10 mm row is blank).
		assertThrows(CalculationException.class, () -> table.lookup(ConduitType.LFMC, 130));
		assertThrows(CalculationException.class, () -> table.lookup(ConduitType.IMC, 10));
	}

	@Test
	void everyRow_hasAFillAreaThatFollowsFromItsTotal() {
		// 40 %, 60 %, 53 % and 31 % of the total area, to the rounding the table prints, and a total that follows from the diameter.
		for (ConduitDimensionEntry row : table.allEntries()) {
			double total = row.totalArea100PercentMm2();
			String where = row.conduitType() + " " + row.racewaySizeMm() + " mm";
			assertEquals(0.40 * total, row.over2Wires40PercentMm2(), Math.max(2.0, 0.012 * total), where + " 40 %");
			assertEquals(0.53 * total, row.oneWire53PercentMm2(), Math.max(2.0, 0.012 * total), where + " 1 wire");
			assertEquals(0.31 * total, row.twoWires53PercentMm2(), Math.max(2.0, 0.012 * total), where + " 2 wires");
			assertEquals(Math.PI / 4 * row.nominalInternalDiameterMm() * row.nominalInternalDiameterMm(), total,
					Math.max(8.0, 0.03 * total), where + " total from the diameter");
		}
	}

	private void assertRow(ConduitType type, int size, double over2, double sixty, double oneWire, double twoWires, double diameter, double total) {
		ConduitDimensionEntry row = table.lookup(type, size);
		String where = type + " " + size + " mm";
		assertEquals(over2, row.over2Wires40PercentMm2(), DELTA, where + " 40 %");
		assertEquals(sixty, row.sixtyPercentMm2(), DELTA, where + " 60 %");
		assertEquals(oneWire, row.oneWire53PercentMm2(), DELTA, where + " 1 wire");
		assertEquals(twoWires, row.twoWires53PercentMm2(), DELTA, where + " 2 wires");
		assertEquals(diameter, row.nominalInternalDiameterMm(), DELTA, where + " ID");
		assertEquals(total, row.totalArea100PercentMm2(), DELTA, where + " total");
	}

	@Test
	void getEntriesForType_returnsAscendingByRacewaySize() {
		List<ConduitDimensionEntry> entries = table.getEntriesForType(ConduitType.EMT);

		assertTrue(entries.size() >= 2);
		for (int i = 1; i < entries.size(); i++) {
			assertTrue(entries.get(i - 1).racewaySizeMm() < entries.get(i).racewaySizeMm());
		}
	}

	@Test
	void lookup_unknownRacewaySize_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> table.lookup(ConduitType.EMT, 9999));
	}

}
