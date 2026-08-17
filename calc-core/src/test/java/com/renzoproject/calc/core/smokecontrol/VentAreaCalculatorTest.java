package com.renzoproject.calc.core.smokecontrol;

import com.renzoproject.calc.core.common.AirProperties;
import com.renzoproject.calc.core.common.AirPropertiesResolver;
import com.renzoproject.calc.core.common.FakeAirPropertiesResolver;
import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VentAreaCalculatorTest {

	private static final double DELTA = 1e-9;
	private static final double SPOT_CHECK_DELTA = 0.01;

	// Mirrors the published reference values from air-properties.json / defaults.json.
	private final AirPropertiesResolver airPropertiesResolver = new FakeAirPropertiesResolver(new AirProperties(1.0, 101325.0, 287.0, 9.81));
	private final SmokeControlDefaultsResolver defaultsResolver = new FakeSmokeControlDefaultsResolver(new SmokeControlDefaults(1.0, 0.7, 0.6));

	private final VentAreaCalculator calculator = new VentAreaCalculator(airPropertiesResolver, defaultsResolver);

	@Test
	void warehouse1ValenzuelaCity_matchesApprovedBodReportSpotCheck() {
		// V = 64.11 m3/s, Ts = 73.59 degC, To = 35 degC, H = 9.5 m, Cd = 0.6.
		VentAreaInput input = new VentAreaInput(64.11, 73.59, 35.0, 9.5, 0.6);

		VentAreaResult result = calculator.calculate(input);

		assertEquals(38.59, result.deltaT(), SPOT_CHECK_DELTA);
		assertEquals(22.11, result.requiredVentArea(), SPOT_CHECK_DELTA);
	}

	@Test
	void omittedDischargeCoefficient_fallsBackToDefaultsResolver() {
		VentAreaInput withDefault = new VentAreaInput(64.11, 73.59, 35.0, 9.5, null);
		VentAreaInput withExplicitMatchingDefault = new VentAreaInput(64.11, 73.59, 35.0, 9.5, 0.6);

		VentAreaResult resultWithDefault = calculator.calculate(withDefault);
		VentAreaResult resultWithExplicit = calculator.calculate(withExplicitMatchingDefault);

		assertEquals(resultWithExplicit.requiredVentArea(), resultWithDefault.requiredVentArea(), DELTA);
	}

	@Test
	void suppliedDischargeCoefficient_overridesDefault() {
		VentAreaInput input = new VentAreaInput(64.11, 73.59, 35.0, 9.5, 0.3);

		VentAreaResult result = calculator.calculate(input);

		// Halving Cd should roughly double the required area vs. the Cd=0.6 spot check.
		assertEquals(44.2213, result.requiredVentArea(), SPOT_CHECK_DELTA);
	}

	@Test
	void nonPositiveVolumetricFlowRate_throws() {
		assertThrows(CalculationException.class, () -> new VentAreaInput(0.0, 73.59, 35.0, 9.5, 0.6));
		assertThrows(CalculationException.class, () -> new VentAreaInput(-64.11, 73.59, 35.0, 9.5, 0.6));
	}

	@Test
	void nonPositiveVentHeight_throws() {
		assertThrows(CalculationException.class, () -> new VentAreaInput(64.11, 73.59, 35.0, 0.0, 0.6));
		assertThrows(CalculationException.class, () -> new VentAreaInput(64.11, 73.59, 35.0, -9.5, 0.6));
	}

	@Test
	void smokeTemperatureAtOrBelowAmbient_throws() {
		assertThrows(CalculationException.class, () -> new VentAreaInput(64.11, 35.0, 35.0, 9.5, 0.6));
		assertThrows(CalculationException.class, () -> new VentAreaInput(64.11, 30.0, 35.0, 9.5, 0.6));
	}

	@Test
	void dischargeCoefficientOutOfRange_throws() {
		assertThrows(CalculationException.class, () -> new VentAreaInput(64.11, 73.59, 35.0, 9.5, 0.0));
		assertThrows(CalculationException.class, () -> new VentAreaInput(64.11, 73.59, 35.0, 9.5, -0.1));
		assertThrows(CalculationException.class, () -> new VentAreaInput(64.11, 73.59, 35.0, 9.5, 1.1));
	}

	@Test
	void dischargeCoefficientExactlyOne_isValid() {
		VentAreaInput input = new VentAreaInput(64.11, 73.59, 35.0, 9.5, 1.0);

		assertEquals(1.0, input.dischargeCoefficient(), DELTA);
	}

}
