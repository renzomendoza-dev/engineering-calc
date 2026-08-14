package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConductorDcResistanceTableTest {

	private static final double DELTA = 1e-9;

	private final ConductorDcResistanceTable table = new ConductorDcResistanceTable();

	@Test
	void lookup_size100CopperUncoated_matchesPublishedValue() {
		double resistance = table.lookupOhmsPerMeter(
				new ConductorSize("100", 100.0), ConductorMaterial.COPPER, false);

		assertEquals(0.0608 / 305.0, resistance, DELTA);
	}

	@Test
	void lookup_size100CopperCoated_matchesPublishedValue() {
		double resistance = table.lookupOhmsPerMeter(
				new ConductorSize("100", 100.0), ConductorMaterial.COPPER, true);

		assertEquals(0.0626 / 305.0, resistance, DELTA);
	}

	@Test
	void lookup_defaultsToStrandedRowWhenBothExist() {
		// size "2.0" has both a solid (1 strand) and stranded (7 strand) row;
		// stranded copper uncoated is 3.14, solid is 3.07 — must pick stranded.
		double resistance = table.lookupOhmsPerMeter(
				new ConductorSize("2.0", 2.0), ConductorMaterial.COPPER, false);

		assertEquals(3.14 / 305.0, resistance, DELTA);
	}

	@Test
	void lookup_sizeWithOnlyStrandedRow_usesIt() {
		// size "14" only has a stranded row in the published table.
		double resistance = table.lookupOhmsPerMeter(
				new ConductorSize("14", 14.0), ConductorMaterial.COPPER, false);

		assertEquals(0.491 / 305.0, resistance, DELTA);
	}

	@Test
	void lookup_aluminum_ignoresCoatedFlag() {
		double resistance = table.lookupOhmsPerMeter(
				new ConductorSize("100", 100.0), ConductorMaterial.ALUMINUM, true);

		assertEquals(0.100 / 305.0, resistance, DELTA);
	}

	@Test
	void lookup_unknownSize_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> table.lookupOhmsPerMeter(
				new ConductorSize("9999", 9999.0), ConductorMaterial.COPPER, false));
	}

	@Test
	void allSizes_includesDcOnlySizeNotPresentInAcTable() {
		// "0.75" only has a DC resistance row — not present in table-10-1-1-9.
		List<ConductorSize> sizes = table.allSizes();

		assertTrue(sizes.stream().anyMatch(size -> size.label().equals("0.75")));
	}

}
