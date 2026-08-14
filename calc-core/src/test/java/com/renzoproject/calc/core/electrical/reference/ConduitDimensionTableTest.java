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
