package com.renzoproject.calc.core.smokecontrol;

import com.renzoproject.calc.core.common.AirProperties;
import com.renzoproject.calc.core.common.AirPropertiesResolver;
import com.renzoproject.calc.core.common.FakeAirPropertiesResolver;
import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TSquaredSmokeProductionCalculatorTest {

	private static final double DELTA = 1e-9;
	private static final double SPOT_CHECK_DELTA = 0.01;

	// Mirrors the published reference values from air-properties.json / defaults.json.
	private final AirPropertiesResolver airPropertiesResolver = new FakeAirPropertiesResolver(new AirProperties(1.0, 101325.0, 287.0, 9.81));
	private final SmokeControlDefaultsResolver defaultsResolver = new FakeSmokeControlDefaultsResolver(new SmokeControlDefaults(1.0, 0.7, 0.6));

	private final TSquaredSmokeProductionCalculator calculator = new TSquaredSmokeProductionCalculator(airPropertiesResolver, defaultsResolver);

	@Test
	void referenceWorkbookExample_matchesCorrectedSpotCheck() {
		// alpha = 0.01 kW/s^2, Qcap = 3600 kW, t = 600 s (alpha*t^2 = 3600 -- exactly at the
		// cap boundary), chi = 0.7, ceilingHeight = 2.5 m, fireBaseHeight = 0, To = 30 degC.
		// Ts/rho/v are corrected against the workbook's own undocumented x0.5 bug on the Qc term.
		TSquaredSmokeProductionInput input = new TSquaredSmokeProductionInput(0.01, 3600.0, 600.0, 0.7, 2.5, 0.0, 30.0, null);

		TSquaredSmokeProductionResult result = calculator.calculate(input);

		assertEquals(600.0, result.evaluationTime(), DELTA);
		assertEquals(3600.0, result.designHeatReleaseRate(), SPOT_CHECK_DELTA);
		assertTrue(result.isGrowthCapped());
		assertEquals(2520.0, result.convectiveHeatReleaseRate(), SPOT_CHECK_DELTA);
		assertEquals(3.808, result.flameHeight(), SPOT_CHECK_DELTA);
		assertEquals(2.5, result.heightAboveFire(), DELTA);
		assertInstanceOf(TSquaredNearField.class, result.plumeRegime());
		assertEquals(8.789, result.plumeRegime().massFlowRateKgS(), SPOT_CHECK_DELTA);
		assertEquals(316.75, result.smokeTemperature(), 0.5);
		assertEquals(0.599, result.smokeDensity(), SPOT_CHECK_DELTA);
		assertEquals(14.68, result.volumetricFlowRate(), SPOT_CHECK_DELTA);
	}

	@Test
	void growthBelowCap_isGrowthCappedIsFalse_usesGrowthCurveValue() {
		// alpha*t^2 = 0.01*100^2 = 100, well below the 3600 kW cap.
		TSquaredSmokeProductionInput input = new TSquaredSmokeProductionInput(0.01, 3600.0, 100.0, 0.7, 2.5, 0.0, 30.0, null);

		TSquaredSmokeProductionResult result = calculator.calculate(input);

		assertFalse(result.isGrowthCapped());
		assertEquals(100.0, result.designHeatReleaseRate(), DELTA);
	}

	@Test
	void growthPastCap_isGrowthCappedIsTrue_designHeatReleaseRateClampsToCap() {
		// alpha*t^2 = 0.01*1000^2 = 100000, far past the 3600 kW cap.
		TSquaredSmokeProductionInput input = new TSquaredSmokeProductionInput(0.01, 3600.0, 1000.0, 0.7, 2.5, 0.0, 30.0, null);

		TSquaredSmokeProductionResult result = calculator.calculate(input);

		assertTrue(result.isGrowthCapped());
		assertEquals(3600.0, result.designHeatReleaseRate(), DELTA);
	}

	@Test
	void evaluationTimeZero_isValid_producesZeroHeatReleaseRate() {
		TSquaredSmokeProductionInput input = new TSquaredSmokeProductionInput(0.01, 3600.0, 0.0, 0.7, 2.5, 0.0, 30.0, null);

		TSquaredSmokeProductionResult result = calculator.calculate(input);

		assertFalse(result.isGrowthCapped());
		assertEquals(0.0, result.designHeatReleaseRate(), DELTA);
	}

	@Test
	void farFieldRegime_whenHeightAboveFireExceedsFlameHeight() {
		// A large, slow-capped fire under a tall ceiling keeps flame height well below the
		// height above the fire, forcing the far-field correlation.
		TSquaredSmokeProductionInput input = new TSquaredSmokeProductionInput(0.001, 500.0, 800.0, 0.7, 20.0, 0.0, 30.0, null);

		TSquaredSmokeProductionResult result = calculator.calculate(input);

		assertInstanceOf(TSquaredFarField.class, result.plumeRegime());
		assertTrue(result.heightAboveFire() > result.flameHeight());
	}

	@Test
	void omittedConvectiveFractionAndKs_fallBackToDefaultsResolver() {
		TSquaredSmokeProductionInput input = new TSquaredSmokeProductionInput(0.01, 3600.0, 600.0, null, 2.5, 0.0, 30.0, null);

		TSquaredSmokeProductionResult result = calculator.calculate(input);

		assertEquals(2520.0, result.convectiveHeatReleaseRate(), SPOT_CHECK_DELTA);
	}

	@Test
	void omittedFireBaseHeight_defaultsToZero() {
		TSquaredSmokeProductionInput input = new TSquaredSmokeProductionInput(0.01, 3600.0, 600.0, 0.7, 2.5, null, 30.0, null);

		assertEquals(0.0, input.fireBaseHeight(), DELTA);
	}

	@Test
	void nonPositiveFireGrowthRate_throws() {
		assertThrows(CalculationException.class, () -> new TSquaredSmokeProductionInput(0.0, 3600.0, 600.0, 0.7, 2.5, 0.0, 30.0, null));
		assertThrows(CalculationException.class, () -> new TSquaredSmokeProductionInput(-0.01, 3600.0, 600.0, 0.7, 2.5, 0.0, 30.0, null));
	}

	@Test
	void nonPositiveCappingHRR_throws() {
		assertThrows(CalculationException.class, () -> new TSquaredSmokeProductionInput(0.01, 0.0, 600.0, 0.7, 2.5, 0.0, 30.0, null));
	}

	@Test
	void negativeEvaluationTime_throws() {
		assertThrows(CalculationException.class, () -> new TSquaredSmokeProductionInput(0.01, 3600.0, -1.0, 0.7, 2.5, 0.0, 30.0, null));
	}

	@Test
	void nonPositiveCeilingHeight_throws() {
		assertThrows(CalculationException.class, () -> new TSquaredSmokeProductionInput(0.01, 3600.0, 600.0, 0.7, 0.0, 0.0, 30.0, null));
	}

	@Test
	void fireBaseHeightAtOrAboveCeilingHeight_throws() {
		assertThrows(CalculationException.class, () -> new TSquaredSmokeProductionInput(0.01, 3600.0, 600.0, 0.7, 2.5, 2.5, 30.0, null));
		assertThrows(CalculationException.class, () -> new TSquaredSmokeProductionInput(0.01, 3600.0, 600.0, 0.7, 2.5, 3.0, 30.0, null));
	}

}
