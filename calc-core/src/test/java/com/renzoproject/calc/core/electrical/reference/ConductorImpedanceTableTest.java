package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConductorImpedanceTableTest {

	private static final double DELTA = 1e-9;

	private final ConductorImpedanceTable table = new ConductorImpedanceTable();

	@Test
	void lookup_size100CopperPvc_matchesPublishedValues() {
		ConductorImpedance impedance = table.lookup(
				new ConductorSize("100", 100.0), ConductorMaterial.COPPER, ConduitMaterial.PVC);

		assertEquals(0.062 / 305.0, impedance.resistanceOhmsPerMeter(), DELTA);
		assertEquals(0.041 / 305.0, impedance.reactanceOhmsPerMeter(), DELTA);
	}

	@Test
	void lookup_steelConduitUsesSteelReactanceColumn() {
		ConductorImpedance impedance = table.lookup(
				new ConductorSize("100", 100.0), ConductorMaterial.COPPER, ConduitMaterial.STEEL);

		assertEquals(0.063 / 305.0, impedance.resistanceOhmsPerMeter(), DELTA);
		assertEquals(0.051 / 305.0, impedance.reactanceOhmsPerMeter(), DELTA);
	}

	@Test
	void lookup_pvcAndAluminumConduitShareSameReactance() {
		ConductorImpedance pvc = table.lookup(
				new ConductorSize("100", 100.0), ConductorMaterial.COPPER, ConduitMaterial.PVC);
		ConductorImpedance aluminumConduit = table.lookup(
				new ConductorSize("100", 100.0), ConductorMaterial.COPPER, ConduitMaterial.ALUMINUM);

		assertEquals(pvc.reactanceOhmsPerMeter(), aluminumConduit.reactanceOhmsPerMeter(), DELTA);
	}

	@Test
	void lookup_missingAluminumDataAtSize2_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> table.lookup(
				new ConductorSize("2.0", 2.0), ConductorMaterial.ALUMINUM, ConduitMaterial.PVC));
	}

	@Test
	void lookup_unknownSize_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> table.lookup(
				new ConductorSize("9999", 9999.0), ConductorMaterial.COPPER, ConduitMaterial.PVC));
	}

	@Test
	void allSizes_returnsNonEmptySortedListIncludingKnownSize() {
		List<ConductorSize> sizes = table.allSizes();

		assertTrue(sizes.stream().anyMatch(size -> size.label().equals("100")));
		for (int i = 1; i < sizes.size(); i++) {
			assertTrue(sizes.get(i - 1).crossSectionMm2() < sizes.get(i).crossSectionMm2());
		}
	}

}
