package com.renzoproject.calc.core.mechanical.tank;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.StandardAtmosphere;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PressureTankCalculatorTest {

	private final PressureTankCalculator calculator = new PressureTankCalculator();

	private static Quantity<VolumetricFlowRate> litresPerMinute(double value) {
		return Quantities.getQuantity(value, PipeUnits.LITRE_PER_MINUTE);
	}

	private static Quantity<Pressure> kilopascal(double value) {
		return Quantities.getQuantity(value * 1000.0, Units.PASCAL);
	}

	private static Quantity<Length> metres(double value) {
		return Quantities.getQuantity(value, Units.METRE);
	}

	@Test
	void calculate_conventional_residentialCase_matchesHandCalculation() {
		// 40 L/min full pump flow, 1 min minimum run time, 140/280 kPa cut-in/cut-out, sea level.
		// drawdownVolume = 40 * 1 = 40 L = 0.04 m3
		// lowAbs = 140000 + 101325 = 241325 Pa; highAbs = 280000 + 101325 = 381325 Pa
		// requiredTankVolume = 0.04 / (1 - 241325/381325) = 0.04 / 0.367142 = 0.108916 m3 = 108.92 L
		PressureTankInput input = new PressureTankInput(
				PumpControlType.CONVENTIONAL, litresPerMinute(40.0), 1.0, kilopascal(140.0), kilopascal(280.0), metres(0.0));

		PressureTankResult result = calculator.calculate(input);

		assertEquals(40.0, result.drawdownVolume().to(Units.LITRE).getValue().doubleValue(), 1e-6);
		assertEquals(241325.0, result.lowPressureAbsolute().to(Units.PASCAL).getValue().doubleValue(), 0.01);
		assertEquals(381325.0, result.highPressureAbsolute().to(Units.PASCAL).getValue().doubleValue(), 0.01);
		assertEquals(108.92, result.requiredTankVolume().to(Units.LITRE).getValue().doubleValue(), 0.5);
		// Realistic residential precharged tank range.
		assertTrue(result.requiredTankVolume().to(Units.LITRE).getValue().doubleValue() > 50.0);
		assertTrue(result.requiredTankVolume().to(Units.LITRE).getValue().doubleValue() < 200.0);
	}

	@Test
	void calculate_vfd_producesMeaningfullySmallerTankThanConventional() {
		// Small minimum/sleep flow, short sleep duration, narrower pressure band -- VFD tanks are
		// meant to be much smaller than an equivalent conventional setup's, not just numerically
		// different.
		PressureTankInput conventional = new PressureTankInput(
				PumpControlType.CONVENTIONAL, litresPerMinute(40.0), 1.0, kilopascal(140.0), kilopascal(280.0), metres(0.0));
		PressureTankInput vfd = new PressureTankInput(
				PumpControlType.VFD, litresPerMinute(5.0), 0.5, kilopascal(380.0), kilopascal(400.0), metres(0.0));

		PressureTankResult conventionalResult = calculator.calculate(conventional);
		PressureTankResult vfdResult = calculator.calculate(vfd);

		double conventionalLiters = conventionalResult.requiredTankVolume().to(Units.LITRE).getValue().doubleValue();
		double vfdLiters = vfdResult.requiredTankVolume().to(Units.LITRE).getValue().doubleValue();

		// Hand calc: drawdownVolume = 5*0.5 = 2.5 L; lowAbs=481325, highAbs=501325;
		// requiredTankVolume = 2.5 / (1 - 481325/501325) = 2.5 / 0.039894 = 62.67 L.
		assertEquals(62.67, vfdLiters, 0.5);
		assertTrue(vfdLiters < conventionalLiters,
				"VFD tank (" + vfdLiters + " L) should be meaningfully smaller than the conventional tank (" + conventionalLiters + " L)");
		assertTrue(vfdLiters < conventionalLiters * 0.75);
	}

	@Test
	void calculate_nonPositiveFlowRate_throws() {
		assertThrows(CalculationException.class, () -> new PressureTankInput(
				PumpControlType.CONVENTIONAL, litresPerMinute(0.0), 1.0, kilopascal(140.0), kilopascal(280.0), metres(0.0)));
	}

	@Test
	void calculate_nonPositiveRunTime_throws() {
		assertThrows(CalculationException.class, () -> new PressureTankInput(
				PumpControlType.CONVENTIONAL, litresPerMinute(40.0), 0.0, kilopascal(140.0), kilopascal(280.0), metres(0.0)));
	}

	@Test
	void calculate_highPressureAtOrBelowLowPressure_throws() {
		assertThrows(CalculationException.class, () -> new PressureTankInput(
				PumpControlType.CONVENTIONAL, litresPerMinute(40.0), 1.0, kilopascal(280.0), kilopascal(280.0), metres(0.0)));
	}

	@Test
	void calculate_reusesStandardAtmosphere_notADuplicatedImplementation() {
		Quantity<Length> altitude = metres(1500.0);
		PressureTankInput input = new PressureTankInput(
				PumpControlType.CONVENTIONAL, litresPerMinute(40.0), 1.0, kilopascal(140.0), kilopascal(280.0), altitude);

		PressureTankResult result = calculator.calculate(input);

		Quantity<Pressure> expectedAtmPressure = StandardAtmosphere.pressureAtAltitude(altitude);
		double expectedLowAbsolutePa = kilopascal(140.0).to(Units.PASCAL).getValue().doubleValue()
				+ expectedAtmPressure.to(Units.PASCAL).getValue().doubleValue();
		double expectedHighAbsolutePa = kilopascal(280.0).to(Units.PASCAL).getValue().doubleValue()
				+ expectedAtmPressure.to(Units.PASCAL).getValue().doubleValue();

		assertEquals(expectedLowAbsolutePa, result.lowPressureAbsolute().to(Units.PASCAL).getValue().doubleValue(), 0.01);
		assertEquals(expectedHighAbsolutePa, result.highPressureAbsolute().to(Units.PASCAL).getValue().doubleValue(), 0.01);
	}

}
