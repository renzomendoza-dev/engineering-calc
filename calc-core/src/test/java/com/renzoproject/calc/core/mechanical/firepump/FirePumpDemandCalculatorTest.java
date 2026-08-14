package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FirePumpDemandCalculatorTest {

	private static final double DELTA = 1e-9;

	private final FirePumpDemandCalculator calculator = new FirePumpDemandCalculator();

	@Test
	void flooded_computesRatedFlowAndPressure() {
		FirePumpDemandInput input = new FirePumpDemandInput(
				Quantities.getQuantity(500.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				SuctionCondition.FLOODED,
				Quantities.getQuantity(20.0, FirePumpUnits.PSI),
				null,
				10.0);

		FirePumpDemandResult result = calculator.calculate(input);

		assertEquals(550.0, result.ratedFlow().to(FirePumpUnits.GPM).getValue().doubleValue(), DELTA);
		assertEquals(80.0, result.ratedPressure().to(FirePumpUnits.PSI).getValue().doubleValue(), DELTA);
	}

	@Test
	void lift_computesRatedPressureWithLiftEquivalent() {
		// liftEquivalent = 10ft * 0.433 = 4.33 psi -> ratedPressure = 90 + 4.33 = 94.33 psi.
		FirePumpDemandInput input = new FirePumpDemandInput(
				Quantities.getQuantity(300.0, FirePumpUnits.GPM),
				Quantities.getQuantity(90.0, FirePumpUnits.PSI),
				SuctionCondition.LIFT,
				null,
				Quantities.getQuantity(10.0, FirePumpUnits.FOOT),
				0.0);

		FirePumpDemandResult result = calculator.calculate(input);

		assertEquals(300.0, result.ratedFlow().to(FirePumpUnits.GPM).getValue().doubleValue(), DELTA);
		assertEquals(94.33, result.ratedPressure().to(FirePumpUnits.PSI).getValue().doubleValue(), DELTA);
	}

	@Test
	void safetyMargin_appliedToFlowOnlyNotPressure() {
		FirePumpDemandInput input = new FirePumpDemandInput(
				Quantities.getQuantity(400.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				SuctionCondition.FLOODED,
				Quantities.getQuantity(10.0, FirePumpUnits.PSI),
				null,
				25.0);

		FirePumpDemandResult result = calculator.calculate(input);

		assertEquals(500.0, result.ratedFlow().to(FirePumpUnits.GPM).getValue().doubleValue(), DELTA);
		assertEquals(90.0, result.ratedPressure().to(FirePumpUnits.PSI).getValue().doubleValue(), DELTA);
	}

	@Test
	void flooded_missingAvailableSuctionPressure_throws() {
		assertThrows(CalculationException.class, () -> new FirePumpDemandInput(
				Quantities.getQuantity(500.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				SuctionCondition.FLOODED,
				null, null, 0.0));
	}

	@Test
	void flooded_withSuctionLiftAlsoProvided_throws() {
		assertThrows(CalculationException.class, () -> new FirePumpDemandInput(
				Quantities.getQuantity(500.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				SuctionCondition.FLOODED,
				Quantities.getQuantity(20.0, FirePumpUnits.PSI),
				Quantities.getQuantity(5.0, FirePumpUnits.FOOT),
				0.0));
	}

	@Test
	void lift_missingSuctionLift_throws() {
		assertThrows(CalculationException.class, () -> new FirePumpDemandInput(
				Quantities.getQuantity(300.0, FirePumpUnits.GPM),
				Quantities.getQuantity(90.0, FirePumpUnits.PSI),
				SuctionCondition.LIFT,
				null, null, 0.0));
	}

	@Test
	void lift_withAvailableSuctionPressureAlsoProvided_throws() {
		assertThrows(CalculationException.class, () -> new FirePumpDemandInput(
				Quantities.getQuantity(300.0, FirePumpUnits.GPM),
				Quantities.getQuantity(90.0, FirePumpUnits.PSI),
				SuctionCondition.LIFT,
				Quantities.getQuantity(5.0, FirePumpUnits.PSI),
				Quantities.getQuantity(10.0, FirePumpUnits.FOOT),
				0.0));
	}

	@Test
	void negativeSafetyMargin_throws() {
		assertThrows(CalculationException.class, () -> new FirePumpDemandInput(
				Quantities.getQuantity(500.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				SuctionCondition.FLOODED,
				Quantities.getQuantity(20.0, FirePumpUnits.PSI),
				null, -5.0));
	}

	@Test
	void nonPositiveRequiredFlow_throws() {
		assertThrows(CalculationException.class, () -> new FirePumpDemandInput(
				Quantities.getQuantity(0.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				SuctionCondition.FLOODED,
				Quantities.getQuantity(20.0, FirePumpUnits.PSI),
				null, 0.0));
	}

	@Test
	void floodedWithSuctionExceedingDemandPressure_throwsOnNonPositiveRatedPressure() {
		// availableSuctionPressure (60psi) > requiredPressureAtDemandPoint (50psi) -> ratedPressure <= 0.
		FirePumpDemandInput input = new FirePumpDemandInput(
				Quantities.getQuantity(500.0, FirePumpUnits.GPM),
				Quantities.getQuantity(50.0, FirePumpUnits.PSI),
				SuctionCondition.FLOODED,
				Quantities.getQuantity(60.0, FirePumpUnits.PSI),
				null, 0.0);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

}
