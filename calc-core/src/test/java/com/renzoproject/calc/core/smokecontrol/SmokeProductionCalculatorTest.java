package com.renzoproject.calc.core.smokecontrol;

import com.renzoproject.calc.core.common.AirProperties;
import com.renzoproject.calc.core.common.AirPropertiesResolver;
import com.renzoproject.calc.core.common.FakeAirPropertiesResolver;
import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeProductionCalculatorTest {

	private static final double DELTA = 1e-9;
	private static final double SPOT_CHECK_DELTA = 0.01;

	// Mirrors the published reference values from air-properties.json / defaults.json.
	private final AirPropertiesResolver airPropertiesResolver = new FakeAirPropertiesResolver(new AirProperties(1.0, 101325.0, 287.0, 9.81));
	private final SmokeControlDefaultsResolver defaultsResolver = new FakeSmokeControlDefaultsResolver(new SmokeControlDefaults(1.0, 0.7, 0.6));

	private final SmokeProductionCalculator calculator = new SmokeProductionCalculator(airPropertiesResolver, defaultsResolver);

	@Test
	void warehouse1ValenzuelaCity_matchesRealProjectSpotCheck() {
		// A = 9 m2 (derived from Q=3600kW / HRR=400kW/m2), HRR = 400 kW/m2, chi = 0.7,
		// ceilingHeight = 12 m, fireBaseHeight = 0, To = 35 degC.
		SmokeProductionInput input = new SmokeProductionInput(9.0, 400.0, 0.7, 12.0, 0.0, 35.0, null);

		SmokeProductionResult result = calculator.calculate(input);

		assertEquals(3600.0, result.designHeatReleaseRate(), DELTA);
		assertEquals(2520.0, result.convectiveHeatReleaseRate(), DELTA);
		assertEquals(3.81, result.flameHeight(), SPOT_CHECK_DELTA);
		assertEquals(12.0, result.heightAboveFire(), DELTA);
		assertInstanceOf(FarField.class, result.plumeRegime());
		assertEquals(65.31, result.plumeRegime().massFlowRateKgS(), SPOT_CHECK_DELTA);
		assertEquals(73.59, result.smokeTemperature(), SPOT_CHECK_DELTA);
		assertEquals(1.019, result.smokeDensity(), SPOT_CHECK_DELTA);
		assertEquals(64.11, result.volumetricFlowRate(), SPOT_CHECK_DELTA);
	}

	@Test
	void omittedConvectiveFractionAndKs_fallBackToDefaultsResolver() {
		// Same worked example, but chi and Ks both omitted -- defaults (0.7, 1.0) should
		// produce the identical result to the explicit spot check above.
		SmokeProductionInput input = new SmokeProductionInput(9.0, 400.0, null, 12.0, 0.0, 35.0, null);

		SmokeProductionResult result = calculator.calculate(input);

		assertEquals(2520.0, result.convectiveHeatReleaseRate(), DELTA);
		assertEquals(65.31, result.plumeRegime().massFlowRateKgS(), SPOT_CHECK_DELTA);
	}

	@Test
	void suppliedConvectiveFractionAndKs_overrideDefaults() {
		SmokeProductionInput input = new SmokeProductionInput(9.0, 400.0, 0.5, 12.0, 0.0, 35.0, 0.8);

		SmokeProductionResult result = calculator.calculate(input);

		// chi=0.5 -> Qc = 3600*0.5 = 1800, not the default chi=0.7's 2520.
		assertEquals(1800.0, result.convectiveHeatReleaseRate(), DELTA);
	}

	@Test
	void omittedFireBaseHeight_defaultsToZero() {
		SmokeProductionInput withNull = new SmokeProductionInput(9.0, 400.0, 0.7, 12.0, null, 35.0, null);
		SmokeProductionInput withZero = new SmokeProductionInput(9.0, 400.0, 0.7, 12.0, 0.0, 35.0, null);

		assertEquals(0.0, withNull.fireBaseHeight(), DELTA);
		assertEquals(calculator.calculate(withZero).heightAboveFire(), calculator.calculate(withNull).heightAboveFire(), DELTA);
	}

	@Test
	void nearFieldRegime_whenHeightAboveFireAtOrBelowFlameHeight() {
		// Large HRR density over a small area concentrated close to a low ceiling drives z_l
		// (flame height) above z (height above fire), forcing the near-field correlation.
		SmokeProductionInput input = new SmokeProductionInput(50.0, 1000.0, 0.7, 5.0, 0.0, 20.0, null);

		SmokeProductionResult result = calculator.calculate(input);

		assertInstanceOf(NearField.class, result.plumeRegime());
		assertTrue(result.plumeRegime().massFlowRateKgS() > 0);
		assertTrue(result.heightAboveFire() <= result.flameHeight());
	}

	@Test
	void nonPositiveDesignFireArea_throws() {
		assertThrows(CalculationException.class, () -> new SmokeProductionInput(0.0, 400.0, 0.7, 12.0, 0.0, 35.0, null));
		assertThrows(CalculationException.class, () -> new SmokeProductionInput(-9.0, 400.0, 0.7, 12.0, 0.0, 35.0, null));
	}

	@Test
	void nonPositiveHeatReleaseRateDensity_throws() {
		assertThrows(CalculationException.class, () -> new SmokeProductionInput(9.0, 0.0, 0.7, 12.0, 0.0, 35.0, null));
	}

	@Test
	void nonPositiveCeilingHeight_throws() {
		assertThrows(CalculationException.class, () -> new SmokeProductionInput(9.0, 400.0, 0.7, 0.0, 0.0, 35.0, null));
	}

	@Test
	void ambientTemperatureOutsideReasonableRange_throws() {
		assertThrows(CalculationException.class, () -> new SmokeProductionInput(9.0, 400.0, 0.7, 12.0, 0.0, -50.0, null));
		assertThrows(CalculationException.class, () -> new SmokeProductionInput(9.0, 400.0, 0.7, 12.0, 0.0, 100.0, null));
	}

	@Test
	void fireBaseHeightAtOrAboveCeilingHeight_throws() {
		assertThrows(CalculationException.class, () -> new SmokeProductionInput(9.0, 400.0, 0.7, 12.0, 12.0, 35.0, null));
		assertThrows(CalculationException.class, () -> new SmokeProductionInput(9.0, 400.0, 0.7, 12.0, 15.0, 35.0, null));
	}

}
