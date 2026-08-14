package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmpacityTableTest {

	private static final double DELTA = 1e-9;

	private final AmpacityTable table = new AmpacityTable();

	@Test
	void lookup_copperSize100At75C_matchesPublishedValue() {
		assertEquals(220.0, table.lookup(ConductorMaterial.COPPER, "100", 75), DELTA);
	}

	@Test
	void lookup_unknownCombination_throwsCalculationException() {
		// size "2.0" has no published aluminum ampacity at all.
		assertThrows(CalculationException.class, () -> table.lookup(ConductorMaterial.ALUMINUM, "2.0", 75));
	}

	@Test
	void lookup_unsupportedTempRating_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> table.lookup(ConductorMaterial.COPPER, "100", 100));
	}

	@Test
	void allSizesSortedAscendingByArea_returnsAscendingDistinctSizes() {
		List<ConductorSize> sizes = table.allSizesSortedAscendingByArea();

		assertTrue(sizes.stream().anyMatch(size -> size.label().equals("100")));
		assertEquals(sizes.size(), sizes.stream().distinct().count());
		for (int i = 1; i < sizes.size(); i++) {
			assertTrue(sizes.get(i - 1).crossSectionMm2() < sizes.get(i).crossSectionMm2());
		}
	}

}
