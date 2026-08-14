package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link JsonPipeDimensionResolver}'s {@link PipeRoughnessResolver} implementation
 * against the real {@code reference/pipes/{material}.json} files (patched with a
 * {@code "hydraulics"} block — see {@code reference/fluids/roughness-patch-instructions.md}),
 * to confirm the loader itself works correctly. {@link PipePressureLossCalculatorTest} uses a
 * fake {@link PipeRoughnessResolver} instead, so it isn't coupled to these exact published
 * numbers.
 */
class PipeRoughnessResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonPipeDimensionResolver resolver = new JsonPipeDimensionResolver();

	@Test
	void resolveAbsoluteRoughnessMm_gi_returnsPublishedValue() {
		assertEquals(0.15, resolver.resolveAbsoluteRoughnessMm("GI"), DELTA);
	}

	@Test
	void resolveAbsoluteRoughnessMm_bi_returnsPublishedValue() {
		assertEquals(0.045, resolver.resolveAbsoluteRoughnessMm("BI"), DELTA);
	}

	@Test
	void resolveAbsoluteRoughnessMm_upvc_returnsPublishedValue() {
		assertEquals(0.0015, resolver.resolveAbsoluteRoughnessMm("UPVC"), DELTA);
	}

	@Test
	void resolveAbsoluteRoughnessMm_ppr_returnsPublishedValue() {
		assertEquals(0.007, resolver.resolveAbsoluteRoughnessMm("PPR"), DELTA);
	}

	@Test
	void resolveAbsoluteRoughnessMm_isCaseInsensitiveOnMaterial() {
		assertEquals(0.15, resolver.resolveAbsoluteRoughnessMm("gi"), DELTA);
	}

	@Test
	void resolveAbsoluteRoughnessMm_unknownMaterial_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolveAbsoluteRoughnessMm("TITANIUM"));
	}

}
