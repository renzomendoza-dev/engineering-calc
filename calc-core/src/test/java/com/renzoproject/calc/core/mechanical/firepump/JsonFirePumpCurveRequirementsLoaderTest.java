package com.renzoproject.calc.core.mechanical.firepump;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link JsonFirePumpCurveRequirementsLoader} against the real
 * {@code curve-requirements.json} reference data.
 */
class JsonFirePumpCurveRequirementsLoaderTest {

	private static final double DELTA = 1e-9;

	@Test
	void load_returnsPublishedPercentages() {
		FirePumpCurveRequirements requirements = new JsonFirePumpCurveRequirementsLoader().load();

		assertEquals(140.0, requirements.churnMaxPercentOfRated(), DELTA);
		assertEquals(150.0, requirements.overloadFlowPercentOfRated(), DELTA);
		assertEquals(65.0, requirements.overloadMinPressurePercentOfRated(), DELTA);
	}

}
