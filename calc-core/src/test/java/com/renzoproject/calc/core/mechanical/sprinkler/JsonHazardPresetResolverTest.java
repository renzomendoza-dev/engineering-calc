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
	void everyRowIsVerifiedAgainstThePrintedStandard() {
		// Every field has now been checked against the 2019 edition: density and area off
		// Figure 19.3.3.1.1, hose and duration from Table 19.3.3.1.2, coverage from
		// Table 10.2.4.2.1. The calculator's warnings and the README both depend on these tags
		// being honest, so this asserts the promotion rather than leaving it to a reader.
		assertTrue(resolver.allEntries().stream().allMatch(row -> "verified".equals(row.confidence())),
				"expected every hazard preset to be tagged 'verified'");
	}

	@Test
	void storedPointIsTheMinimumAreaEndOfEachCurve() {
		// The curves slope from a lower density over a larger area to a higher density over the
		// minimum area; the stored pair is that minimum-area end. Light/Ordinary bottom out at
		// 1500 ft2 and Extra at 2500 ft2, per 19.3.3.1.4.
		assertEquals(1500.0, resolver.resolve(SprinklerHazard.LIGHT).designAreaSqFt(), 1e-9);
		assertEquals(1500.0, resolver.resolve(SprinklerHazard.ORDINARY_GROUP_2).designAreaSqFt(), 1e-9);
		assertEquals(2500.0, resolver.resolve(SprinklerHazard.EXTRA_GROUP_1).designAreaSqFt(), 1e-9);
		assertEquals(2500.0, resolver.resolve(SprinklerHazard.EXTRA_GROUP_2).designAreaSqFt(), 1e-9);
	}

}
