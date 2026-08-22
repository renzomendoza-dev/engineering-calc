package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link JsonDuctRoughnessResolver} against the real {@code duct-roughness.json} reference
 * data.
 */
class JsonDuctRoughnessResolverTest {

	private static final double DELTA = 1e-9;

	private final JsonDuctRoughnessResolver resolver = new JsonDuctRoughnessResolver();

	@Test
	void resolveAbsoluteRoughnessMm_galvanizedSteelSpiral_matchesAshraeExample6CrossCheck() {
		assertEquals(0.12, resolver.resolveAbsoluteRoughnessMm("GALVANIZED_STEEL_SPIRAL"), DELTA);
	}

	@Test
	void resolveAbsoluteRoughnessMm_flexibleDuctStretched_matchesAshraeExample6CrossCheck() {
		assertEquals(0.9, resolver.resolveAbsoluteRoughnessMm("FLEXIBLE_DUCT_STRETCHED"), DELTA);
	}

	@Test
	void resolveAbsoluteRoughnessMm_pvcDuct_matchesPublishedValue() {
		assertEquals(0.046, resolver.resolveAbsoluteRoughnessMm("PVC_DUCT"), DELTA);
	}

	@Test
	void resolveAbsoluteRoughnessMm_unknownMaterial_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolveAbsoluteRoughnessMm("NOT_A_REAL_MATERIAL"));
	}

	@Test
	void resolveAbsoluteRoughnessMm_null_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolveAbsoluteRoughnessMm(null));
	}

}
