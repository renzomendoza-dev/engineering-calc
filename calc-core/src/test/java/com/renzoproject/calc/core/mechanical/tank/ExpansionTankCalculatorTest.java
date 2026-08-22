package com.renzoproject.calc.core.mechanical.tank;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.FluidPropertiesResolver;
import com.renzoproject.calc.core.mechanical.pipe.JsonFluidPropertiesResolver;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Volume;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpansionTankCalculatorTest {

	private final FluidPropertiesResolver fluidPropertiesResolver = new JsonFluidPropertiesResolver();
	private final ExpansionTankCalculator calculator = new ExpansionTankCalculator(fluidPropertiesResolver);

	private static Quantity<Volume> litres(double value) {
		return Quantities.getQuantity(value, Units.LITRE);
	}

	private static Quantity<Length> metres(double value) {
		return Quantities.getQuantity(value, Units.METRE);
	}

	private static Quantity<Temperature> celsius(double value) {
		return Quantities.getQuantity(value, Units.CELSIUS);
	}

	private static Quantity<Pressure> pascal(double value) {
		return Quantities.getQuantity(value, Units.PASCAL);
	}

	@Test
	void calculate_domesticWaterHeater_matchesHandCalculation() {
		// 190 L water heater, 10m of 20mm piping, 10 degC cold / 60 degC hot (both exact
		// water.json rows -- 999.7 / 983.2 kg/m3), 300 kPa fill / 550 kPa max operating, sea level.
		//
		// pipingVolume = pi/4 * 0.02^2 * 10 = 0.0031415927 m3
		// derivedSystemVolume = 0.19 + 0.0031415927 = 0.1931415927 m3
		// expansionVolume = 0.1931415927 * (999.7/983.2 - 1) = 0.1931415927 * 0.0167819 = 0.0032413 m3
		// fillAbs = 300000 + 101325 = 401325 Pa; maxAbs = 550000 + 101325 = 651325 Pa
		// requiredTankVolume = 0.0032413 / (1 - 401325/651325) = 0.0032413 / 0.383833 = 0.0084445 m3
		ExpansionTankInput input = new ExpansionTankInput(
				SystemType.DOMESTIC_WATER_HEATER,
				litres(190.0),
				metres(10.0),
				20.0,
				1.0,
				celsius(10.0),
				celsius(60.0),
				pascal(300000.0),
				pascal(550000.0),
				metres(0.0));

		ExpansionTankResult result = calculator.calculate(input);

		assertEquals(0.1931416, result.derivedSystemVolume().to(Units.CUBIC_METRE).getValue().doubleValue(), 0.00001);
		assertEquals(0.0032413, result.expansionVolume().to(Units.CUBIC_METRE).getValue().doubleValue(), 0.00005);
		assertEquals(0.0084445, result.requiredTankVolume().to(Units.CUBIC_METRE).getValue().doubleValue(), 0.0003);
		assertEquals(999.7, result.coldWaterDensityKgM3(), 0.01);
		assertEquals(983.2, result.hotWaterDensityKgM3(), 0.01);
		assertEquals(401325.0, result.fillPressureAbsolute().to(Units.PASCAL).getValue().doubleValue(), 1.0);
		assertEquals(651325.0, result.maxOperatingPressureAbsolute().to(Units.PASCAL).getValue().doubleValue(), 1.0);
		assertTrue(result.requiredTankVolume().getValue().doubleValue() > result.expansionVolume().getValue().doubleValue());
	}

	@Test
	void calculate_hydronicHeating_largerSystemHigherTemperature_producesReasonableResult() {
		// 500 L boiler water content, 50m of 25mm piping, 10 degC cold / 90 degC hot (965.3 kg/m3
		// at 90 degC), 150 kPa fill / 300 kPa max operating, sea level.
		ExpansionTankInput input = new ExpansionTankInput(
				SystemType.HYDRONIC_HEATING,
				litres(500.0),
				metres(50.0),
				25.0,
				1.0,
				celsius(10.0),
				celsius(90.0),
				pascal(150000.0),
				pascal(300000.0),
				metres(0.0));

		ExpansionTankResult result = calculator.calculate(input);

		double pipingVolumeM3 = (Math.PI / 4.0) * 0.025 * 0.025 * 50.0;
		double expectedDerivedSystemVolumeM3 = 0.5 + pipingVolumeM3;
		assertEquals(expectedDerivedSystemVolumeM3, result.derivedSystemVolume().to(Units.CUBIC_METRE).getValue().doubleValue(), 1e-9);

		assertEquals(999.7, result.coldWaterDensityKgM3(), 0.01);
		assertEquals(965.3, result.hotWaterDensityKgM3(), 0.01);
		assertTrue(result.expansionVolume().to(Units.CUBIC_METRE).getValue().doubleValue() > 0);
		assertTrue(result.requiredTankVolume().to(Units.CUBIC_METRE).getValue().doubleValue()
				> result.expansionVolume().to(Units.CUBIC_METRE).getValue().doubleValue());
	}

	@Test
	void calculate_additionalVolumeFactorAppliedCorrectly() {
		ExpansionTankInput baseline = new ExpansionTankInput(
				SystemType.DOMESTIC_WATER_HEATER,
				litres(190.0),
				metres(10.0),
				20.0,
				1.0,
				celsius(10.0),
				celsius(60.0),
				pascal(300000.0),
				pascal(550000.0),
				metres(0.0));
		ExpansionTankInput scaled = new ExpansionTankInput(
				SystemType.DOMESTIC_WATER_HEATER,
				litres(190.0),
				metres(10.0),
				20.0,
				1.1,
				celsius(10.0),
				celsius(60.0),
				pascal(300000.0),
				pascal(550000.0),
				metres(0.0));

		double baselineVolumeM3 = calculator.calculate(baseline).derivedSystemVolume().to(Units.CUBIC_METRE).getValue().doubleValue();
		double scaledVolumeM3 = calculator.calculate(scaled).derivedSystemVolume().to(Units.CUBIC_METRE).getValue().doubleValue();

		assertEquals(baselineVolumeM3 * 1.1, scaledVolumeM3, 1e-9);
	}

	@Test
	void construct_nonPositiveVesselVolume_throws() {
		assertThrows(CalculationException.class, () -> new ExpansionTankInput(
				SystemType.DOMESTIC_WATER_HEATER,
				litres(0.0),
				metres(10.0),
				20.0,
				1.0,
				celsius(10.0),
				celsius(60.0),
				pascal(300000.0),
				pascal(550000.0),
				metres(0.0)));
	}

	@Test
	void construct_hotTemperatureAtOrBelowColdTemperature_throws() {
		assertThrows(CalculationException.class, () -> new ExpansionTankInput(
				SystemType.DOMESTIC_WATER_HEATER,
				litres(190.0),
				metres(10.0),
				20.0,
				1.0,
				celsius(60.0),
				celsius(60.0),
				pascal(300000.0),
				pascal(550000.0),
				metres(0.0)));
	}

	@Test
	void construct_maxOperatingPressureAtOrBelowFillPressure_throws() {
		assertThrows(CalculationException.class, () -> new ExpansionTankInput(
				SystemType.DOMESTIC_WATER_HEATER,
				litres(190.0),
				metres(10.0),
				20.0,
				1.0,
				celsius(10.0),
				celsius(60.0),
				pascal(550000.0),
				pascal(550000.0),
				metres(0.0)));
	}

	@Test
	void calculate_outOfRangeTemperature_propagatesFluidPropertiesResolverException() {
		ExpansionTankInput input = new ExpansionTankInput(
				SystemType.DOMESTIC_WATER_HEATER,
				litres(190.0),
				metres(10.0),
				20.0,
				1.0,
				celsius(10.0),
				celsius(150.0),
				pascal(300000.0),
				pascal(550000.0),
				metres(0.0));

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

}
