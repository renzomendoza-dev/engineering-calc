package com.renzoproject.calc.core.mechanical.sprinkler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonHazardPresetResolverTest {

	private final JsonHazardPresetResolver resolver = new JsonHazardPresetResolver();

	@Test
	void allEntries_hasOnePresetPerHazard() {
		assertEquals(SprinklerHazard.values().length, resolver.allEntries().size());
	}

	@Test
	void resolve_lightHazard_returnsTheCurveMinimumPoint() {
		HazardPresetRow row = resolver.resolve(SprinklerHazard.LIGHT);

		assertEquals(0.10, row.densityGpmPerSqFt(), 1e-9);
		assertEquals(1500.0, row.designAreaSqFt(), 1e-9);
		assertEquals(100.0, row.hoseStreamGpm(), 1e-9);
		assertEquals(30, row.durationMinutes());
		assertEquals(225.0, row.maxCoverageSqFtPerSprinkler(), 1e-9);
	}

	@Test
	void resolve_extraHazardGroup2_isTheDensestPreset() {
		HazardPresetRow row = resolver.resolve(SprinklerHazard.EXTRA_GROUP_2);

		assertEquals(0.40, row.densityGpmPerSqFt(), 1e-9);
		assertEquals(2500.0, row.designAreaSqFt(), 1e-9);
	}

	@Test
	void densityIncreasesWithHazard() {
		assertTrue(resolver.resolve(SprinklerHazard.LIGHT).densityGpmPerSqFt()
				< resolver.resolve(SprinklerHazard.ORDINARY_GROUP_1).densityGpmPerSqFt());
		assertTrue(resolver.resolve(SprinklerHazard.ORDINARY_GROUP_1).densityGpmPerSqFt()
				< resolver.resolve(SprinklerHazard.ORDINARY_GROUP_2).densityGpmPerSqFt());
		assertTrue(resolver.resolve(SprinklerHazard.ORDINARY_GROUP_2).densityGpmPerSqFt()
				< resolver.resolve(SprinklerHazard.EXTRA_GROUP_1).densityGpmPerSqFt());
	}

	@Test
	void everyRowIsTaggedPendingVerification() {
		// Nothing in this file has been checked against the printed standard yet; the calculator's
		// warnings and reference/sprinkler/README.md both depend on these tags being honest.
		assertTrue(resolver.allEntries().stream().allMatch(row -> "verify".equals(row.confidence())),
				"expected every hazard preset to be tagged 'verify'");
	}

}
