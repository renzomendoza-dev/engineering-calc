package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConductorDimensionTableTest {

	private static final double DELTA = 1e-9;

	private final ConductorDimensionTable table = new ConductorDimensionTable();

	@Test
	void lookup_thhnSize100_matchesPublishedValue() {
		assertEquals(208.7, table.lookup(InsulationType.THHN, "100"), DELTA);
	}

	@Test
	void lookup_defaultsToWithoutOuterCoveringFalse() {
		// RHH size "2.0" has both variants: false=18.9, true=13.2 — 2-arg lookup must pick false.
		assertEquals(18.9, table.lookup(InsulationType.RHH, "2.0"), DELTA);
	}

	@Test
	void lookup_explicitWithoutOuterCoveringTrue() {
		assertEquals(13.2, table.lookup(InsulationType.RHH, "2.0", true), DELTA);
	}

	@Test
	void lookup_unknownSize_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> table.lookup(InsulationType.THHN, "9999"));
	}

	@Test
	void sizeLabelsFor_returnsAscendingDistinctSizesForType() {
		List<String> sizes = table.sizeLabelsFor(InsulationType.THHN);

		assertTrue(sizes.contains("100"));
		assertEquals(sizes.size(), sizes.stream().distinct().count());
		for (int i = 1; i < sizes.size(); i++) {
			assertTrue(Double.parseDouble(sizes.get(i - 1)) < Double.parseDouble(sizes.get(i)));
		}
	}

}
