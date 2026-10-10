package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

}
