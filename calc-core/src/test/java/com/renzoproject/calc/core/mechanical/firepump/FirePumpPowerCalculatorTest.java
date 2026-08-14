package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirePumpPowerCalculatorTest {

	private static final double DELTA = 1e-9;

	private final FirePumpMotorSizeResolver motorSizeResolver =
			new FakeFirePumpMotorSizeResolver(List.of(10.0, 15.0, 20.0, 25.0, 30.0, 40.0, 50.0));
	private final FirePumpPowerCalculator calculator = new FirePumpPowerCalculator(motorSizeResolver);

	private static PumpCurvePoint point(double gpm, double psi) {
		return new PumpCurvePoint(Quantities.getQuantity(gpm, FirePumpUnits.GPM), Quantities.getQuantity(psi, FirePumpUnits.PSI));
	}

	@Test
	void ratedPointOnly_electric_evaluatesSinglePointAndFlagsOverloadNotChecked() {
		// headFeet=100*2.31=231; whp=(500*231*1)/3960=29.166666666666668; bhp=whp/0.75=38.88888888888889.
		RatedPointOnly input = new RatedPointOnly(
				Quantities.getQuantity(500.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				0.75, 1.0, DriverType.ELECTRIC);

		FirePumpPowerResult result = calculator.calculate(input);

		assertEquals(1, result.evaluatedPoints().size());
		assertEquals(29.166666666666668, result.evaluatedPoints().get(0).waterHorsepower(), DELTA);
		assertEquals(38.88888888888889, result.evaluatedPoints().get(0).brakeHorsepower(), DELTA);
		assertEquals(38.88888888888889, result.maxBrakeHorsepower(), DELTA);
		assertEquals("40 HP", result.recommendedElectricMotorSize());
		assertTrue(result.driverSizingNote().contains("rated-point-only"));
	}

	@Test
	void fullCurve_electric_picksHigherOf150PercentBhpOverRatedBhp() {
		// rated: 500 GPM @ 100psi -> bhp=38.88888888888889 (same as above).
		// 150% flow = 750 GPM, curve gives exactly 75psi there: headFeet=75*2.31=173.25,
		// whp=(750*173.25*1)/3960=32.8125, bhp=32.8125/0.75=43.75 -- higher than the rated bhp.
		CandidatePumpCurve curve = new CandidatePumpCurve(
				Quantities.getQuantity(500.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				List.of(point(0, 135), point(250, 120), point(500, 100), point(750, 75), point(900, 50)));
		FullCurve input = new FullCurve(curve, 0.75, 1.0, DriverType.ELECTRIC);

		FirePumpPowerResult result = calculator.calculate(input);

		assertEquals(2, result.evaluatedPoints().size());
		assertEquals(38.88888888888889, result.evaluatedPoints().get(0).brakeHorsepower(), DELTA);
		assertEquals(43.75, result.evaluatedPoints().get(1).brakeHorsepower(), DELTA);
		assertEquals(43.75, result.maxBrakeHorsepower(), DELTA);
		assertEquals("50 HP", result.recommendedElectricMotorSize());
		assertNull(result.driverSizingNote());
	}

	@Test
	void ratedPointOnly_diesel_noMotorSizeAndBothCaveatsPresent() {
		RatedPointOnly input = new RatedPointOnly(
				Quantities.getQuantity(500.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				0.75, 1.0, DriverType.DIESEL);

		FirePumpPowerResult result = calculator.calculate(input);

		assertNull(result.recommendedElectricMotorSize());
		assertTrue(result.driverSizingNote().contains("rated-point-only"));
		assertTrue(result.driverSizingNote().contains("Diesel driver"));
	}

	@Test
	void motorSizeExceedsLargestFakeStep_throwsRatherThanClamping() {
		RatedPointOnly input = new RatedPointOnly(
				Quantities.getQuantity(2000.0, FirePumpUnits.GPM),
				Quantities.getQuantity(200.0, FirePumpUnits.PSI),
				0.75, 1.0, DriverType.ELECTRIC);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

}
