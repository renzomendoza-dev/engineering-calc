package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConductorPropertiesResolverTest {

	private static final double DELTA = 1e-9;

	private final ConductorPropertiesResolver resolver = new ConductorPropertiesResolver();

	@Test
	void resolve_dcCircuit_usesDcTableAndZeroesReactance() {
		ConductorProperties properties = resolver.resolve(
				CircuitType.DC, new ConductorSize("100", 100.0), ConductorMaterial.COPPER, ConduitMaterial.PVC);

		assertEquals(0.0608 / 305.0, properties.resistanceOhmsPerMeter(), DELTA);
		assertEquals(0.0, properties.reactanceOhmsPerMeter(), DELTA);
	}

	@Test
	void resolve_singlePhaseAc_usesImpedanceTableForBothResistanceAndReactance() {
		ConductorProperties properties = resolver.resolve(
				CircuitType.SINGLE_PHASE_AC, new ConductorSize("100", 100.0), ConductorMaterial.COPPER, ConduitMaterial.PVC);

		assertEquals(0.062 / 305.0, properties.resistanceOhmsPerMeter(), DELTA);
		assertEquals(0.041 / 305.0, properties.reactanceOhmsPerMeter(), DELTA);
	}

	@Test
	void resolve_threePhaseAc_usesImpedanceTableForBothResistanceAndReactance() {
		ConductorProperties properties = resolver.resolve(
				CircuitType.THREE_PHASE_AC, new ConductorSize("100", 100.0), ConductorMaterial.COPPER, ConduitMaterial.STEEL);

		assertEquals(0.063 / 305.0, properties.resistanceOhmsPerMeter(), DELTA);
		assertEquals(0.051 / 305.0, properties.reactanceOhmsPerMeter(), DELTA);
	}

	@Test
	void allSizes_unionsBothTablesAndDedupesOverlappingLabels() {
		List<ConductorSize> sizes = resolver.allSizes();
		List<String> labels = sizes.stream().map(ConductorSize::label).collect(Collectors.toList());

		// "0.75" only exists in the DC table, "100" exists in both — must appear once.
		assertTrue(labels.contains("0.75"));
		assertEquals(1, labels.stream().filter(label -> label.equals("100")).count());
	}

}
