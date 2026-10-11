package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonCFactorResolverTest {

	private final JsonCFactorResolver resolver = new JsonCFactorResolver();

	@Test
	void steel_wetSystem_is120() {
		assertEquals(120.0, resolver.resolve("STEEL", SprinklerSystemType.WET).cFactor(), 1e-9);
	}

	@Test
	void steel_dryAndPreaction_are100() {
		assertEquals(100.0, resolver.resolve("STEEL", SprinklerSystemType.DRY).cFactor(), 1e-9);
		assertEquals(100.0, resolver.resolve("STEEL", SprinklerSystemType.PREACTION).cFactor(), 1e-9);
	}

	@Test
	void copperAndCpvc_are150_regardlessOfServiceType() {
		assertEquals(150.0, resolver.resolve("COPPER", SprinklerSystemType.WET).cFactor(), 1e-9);
		assertEquals(150.0, resolver.resolve("CPVC", SprinklerSystemType.DRY).cFactor(), 1e-9);
	}

	@Test
	void undergroundDuctileIron_is140() {
		assertEquals(140.0, resolver.resolve("DUCTILE_IRON_UNDERGROUND", SprinklerSystemType.WET).cFactor(), 1e-9);
	}

	@Test
	void resolve_isCaseInsensitiveOnPipeType() {
		assertEquals(120.0, resolver.resolve("steel", SprinklerSystemType.WET).cFactor(), 1e-9);
	}

	@Test
	void resolve_unknownPipeType_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolve("BAMBOO", SprinklerSystemType.WET));
	}

	@Test
	void resolve_nullSystemType_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolve("STEEL", null));
	}

	@Test
	void epsilonFactors_matchAnnexTableA27248point2() {
		// The annex pairs each C with an equivalent absolute roughness. Recorded for reference;
		// Hazen-Williams itself only uses C.
		assertEquals(0.004, resolver.resolve("STEEL", SprinklerSystemType.WET).epsilonFactorInch(), 1e-12);
		assertEquals(0.100, resolver.resolve("STEEL", SprinklerSystemType.WET).epsilonFactorMm(), 1e-12);
		assertEquals(0.015, resolver.resolve("STEEL", SprinklerSystemType.DRY).epsilonFactorInch(), 1e-12);
		assertEquals(0.375, resolver.resolve("STEEL", SprinklerSystemType.DRY).epsilonFactorMm(), 1e-12);
		assertEquals(0.000084, resolver.resolve("COPPER", SprinklerSystemType.WET).epsilonFactorInch(), 1e-12);
		assertEquals(0.0021, resolver.resolve("COPPER", SprinklerSystemType.WET).epsilonFactorMm(), 1e-12);
	}

	@Test
	void rowsNotCoveredByTheAnnex_haveNoEpsilon() {
		// Epsilon comes from the annex roughness table, which covers fewer materials than the
		// normative C table. A material the annex omits carries null, never an invented figure.
		assertEquals(null, resolver.resolve("DUCTILE_IRON_UNDERGROUND", SprinklerSystemType.WET).epsilonFactorInch());
		assertEquals(null, resolver.resolve("STAINLESS_STEEL", SprinklerSystemType.WET).epsilonFactorInch());
		assertEquals(null, resolver.resolve("CONCRETE", SprinklerSystemType.WET).epsilonFactorMm());
	}

	@Test
	void everyRowIsVerifiedAgainstTheNormativeTable() {
		// Table 27.2.4.8.1 has been read from the printed 2019 edition in full, including the two
		// rows previously carried as this app's own mapping (preaction-as-dry, CPVC-as-plastic)
		// and the ductile iron row that had no source at all.
		assertTrue(resolver.allEntries().stream().allMatch(row -> "verified".equals(row.confidence())),
				"expected every C factor row to be tagged 'verified'");
	}

	@Test
	void unlinedCastOrDuctileIron_is100_notTheCementLined140() {
		// The lining is what carries the 140. Resolving an unlined main to 140 would
		// under-predict friction loss badly, so the two are separate pipe types.
		assertEquals(100.0, resolver.resolve("UNLINED_CAST_OR_DUCTILE_IRON", SprinklerSystemType.WET).cFactor(), 1e-9);
		assertEquals(140.0, resolver.resolve("DUCTILE_IRON_UNDERGROUND", SprinklerSystemType.WET).cFactor(), 1e-9);
	}

	@Test
	void stainlessAndBrass_are150_notTheCarbonSteelValue() {
		// Table 27.2.4.8.1 groups copper, brass and stainless at 150; carbon steel is 120/100.
		assertEquals(150.0, resolver.resolve("STAINLESS_STEEL", SprinklerSystemType.DRY).cFactor(), 1e-9);
		assertEquals(150.0, resolver.resolve("BRASS", SprinklerSystemType.WET).cFactor(), 1e-9);
	}

	@Test
	void allEntries_coversEveryPublishedRow() {
		// Ten rows are printed; copper/brass/stainless share one of them and this file splits it
		// into three, while black and galvanized steel share our generic STEEL rows.
		assertEquals(11, resolver.allEntries().size());
	}

}
