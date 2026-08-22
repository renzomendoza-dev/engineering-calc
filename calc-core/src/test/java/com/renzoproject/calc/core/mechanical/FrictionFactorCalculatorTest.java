package com.renzoproject.calc.core.mechanical;

import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct unit tests for the extracted utility, pinned to the exact same regression values
 * {@code PipePressureLossCalculatorTest} already validates indirectly through
 * {@code PipePressureLossCalculator} -- confirms this is a pure extraction at the utility's own
 * layer, not just via its one (former) caller.
 */
class FrictionFactorCalculatorTest {

	@Test
	void frictionFactor_swameeJain_matchesPipePressureLossCalculatorTestRegressionValue() {
		// Re=254647.90894703253, relativeRoughness=0.001 (0.1mm roughness / 100mm diameter) --
		// same scenario as PipePressureLossCalculatorTest.turbulentFlow_swameeJain_matchesHandVerifiedValues.
		double frictionFactor = FrictionFactorCalculator.frictionFactor(254647.90894703253, 0.001, FrictionFactorMethod.SWAMEE_JAIN);

		assertEquals(0.02090989762777186, frictionFactor, 1e-9);
	}

	@Test
	void frictionFactor_colebrookWhite_matchesPipePressureLossCalculatorTestRegressionValue() {
		double frictionFactor = FrictionFactorCalculator.frictionFactor(254647.90894703253, 0.001, FrictionFactorMethod.COLEBROOK_WHITE);

		assertEquals(0.0207600530848401, frictionFactor, 1e-9);
	}

	@Test
	void frictionFactor_transitionalFlow_matchesPipePressureLossCalculatorTestRegressionValue() {
		// Re=3000.0 -- same scenario as PipePressureLossCalculatorTest.transitionalFlow_...
		double frictionFactor = FrictionFactorCalculator.frictionFactor(3000.0, 0.001, FrictionFactorMethod.SWAMEE_JAIN);

		assertEquals(0.04550962445356021, frictionFactor, 1e-9);
	}

	@Test
	void frictionFactor_bothMethods_agreeWithinTwoPercentForTypicalTurbulentFlow() {
		double swameeJain = FrictionFactorCalculator.frictionFactor(254647.90894703253, 0.001, FrictionFactorMethod.SWAMEE_JAIN);
		double colebrookWhite = FrictionFactorCalculator.frictionFactor(254647.90894703253, 0.001, FrictionFactorMethod.COLEBROOK_WHITE);

		double relativeDiff = Math.abs(colebrookWhite - swameeJain) / swameeJain;
		assertTrue(relativeDiff < 0.02, "Expected agreement within 2%, was " + relativeDiff);
	}

}
