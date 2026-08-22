package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.FluidProperties;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticalAirPropertiesResolverTest {

	private final AnalyticalAirPropertiesResolver resolver = new AnalyticalAirPropertiesResolver();

	private static Quantity<Temperature> celsius(double value) {
		return Quantities.getQuantity(value, Units.CELSIUS);
	}

	private static Quantity<Length> metres(double value) {
		return Quantities.getQuantity(value, Units.METRE);
	}

	@Test
	void resolve_standardConditions_matchesAshraeChapter3RegressionValues() {
		// ASHRAE Fundamentals Ch.3 standard air: 20 degC, sea level -> density 1.21 kg/m3,
		// viscosity 18.1 uN*s/m2 (18.1e-6 Pa*s). Regression check against reference/duct/README.md's
		// documented cross-validation (density within normal rounding, viscosity almost exact).
		FluidProperties properties = resolver.resolve(celsius(20.0), metres(0.0));

		assertEquals(1.204, properties.densityKgM3(), 0.001);
		assertEquals(18.13e-6, properties.dynamicViscosityPaS(), 0.01e-6);
	}

	@Test
	void resolve_higherAltitude_producesLowerDensity() {
		FluidProperties seaLevel = resolver.resolve(celsius(20.0), metres(0.0));
		FluidProperties highAltitude = resolver.resolve(celsius(20.0), metres(1500.0));

		assertTrue(highAltitude.densityKgM3() < seaLevel.densityKgM3());
	}

	@Test
	void resolve_higherTemperature_producesLowerDensityAndHigherViscosity() {
		FluidProperties cooler = resolver.resolve(celsius(20.0), metres(0.0));
		FluidProperties warmer = resolver.resolve(celsius(40.0), metres(0.0));

		assertTrue(warmer.densityKgM3() < cooler.densityKgM3());
		assertTrue(warmer.dynamicViscosityPaS() > cooler.dynamicViscosityPaS());
	}

	@Test
	void resolve_nullTemperature_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolve(null, metres(0.0)));
	}

	@Test
	void resolve_nullAltitude_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolve(celsius(20.0), null));
	}

}
