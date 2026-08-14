package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirePumpCurveValidationCalculatorTest {

	private static final double DELTA = 1e-9;

	// Fake requirements matching NFPA 20's published figures, but as a fake test double -- not
	// read from the real curve-requirements.json, so this test isn't coupled to it.
	private final FirePumpCurveRequirementsLoader requirementsLoader =
			new FakeFirePumpCurveRequirementsLoader(new FirePumpCurveRequirements(140.0, 150.0, 65.0));
	private final FirePumpCurveValidationCalculator calculator = new FirePumpCurveValidationCalculator(requirementsLoader);

	private static PumpCurvePoint point(double gpm, double psi) {
		return new PumpCurvePoint(Quantities.getQuantity(gpm, FirePumpUnits.GPM), Quantities.getQuantity(psi, FirePumpUnits.PSI));
	}

	@Test
	void compliantCurve_allChecksPass() {
		// rated 1000 GPM @ 100 psi. churn @0=135psi (<=140% of 100=140 -> compliant).
		// overload @150%=1500 GPM=70psi (>=65% of 100=65 -> compliant).
		CandidatePumpCurve candidate = new CandidatePumpCurve(
				Quantities.getQuantity(1000.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				List.of(point(0, 135), point(500, 115), point(1000, 100), point(1500, 70)));
		FirePumpCurveValidationInput input = new FirePumpCurveValidationInput(
				candidate, Quantities.getQuantity(1000.0, FirePumpUnits.GPM), Quantities.getQuantity(100.0, FirePumpUnits.PSI));

		FirePumpCurveValidationResult result = calculator.calculate(input);

		assertTrue(result.meetsCapacityDemand());
		assertEquals(135.0, result.churnPressure().to(FirePumpUnits.PSI).getValue().doubleValue(), DELTA);
		assertTrue(result.churnCompliant());
		assertEquals(70.0, result.overloadPressure().to(FirePumpUnits.PSI).getValue().doubleValue(), DELTA);
		assertTrue(result.overloadCompliant());
		assertTrue(result.overallCompliant());
	}

	@Test
	void churnPressureTooHigh_notCompliant() {
		// churn @0=145psi > 140% of 100=140psi ceiling -> non-compliant.
		CandidatePumpCurve candidate = new CandidatePumpCurve(
				Quantities.getQuantity(1000.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				List.of(point(0, 145), point(500, 115), point(1000, 100), point(1500, 70)));
		FirePumpCurveValidationInput input = new FirePumpCurveValidationInput(
				candidate, Quantities.getQuantity(1000.0, FirePumpUnits.GPM), Quantities.getQuantity(100.0, FirePumpUnits.PSI));

		FirePumpCurveValidationResult result = calculator.calculate(input);

		assertFalse(result.churnCompliant());
		assertFalse(result.overallCompliant());
	}

	@Test
	void overloadPressureTooLow_notCompliant() {
		// overload @150%=1500 GPM=60psi < 65% of 100=65psi floor -> non-compliant.
		CandidatePumpCurve candidate = new CandidatePumpCurve(
				Quantities.getQuantity(1000.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				List.of(point(0, 135), point(500, 115), point(1000, 100), point(1500, 60)));
		FirePumpCurveValidationInput input = new FirePumpCurveValidationInput(
				candidate, Quantities.getQuantity(1000.0, FirePumpUnits.GPM), Quantities.getQuantity(100.0, FirePumpUnits.PSI));

		FirePumpCurveValidationResult result = calculator.calculate(input);

		assertFalse(result.overloadCompliant());
		assertFalse(result.overallCompliant());
	}

	@Test
	void curveDoesNotBracket150PercentFlow_throwsRatherThanExtrapolating() {
		// Curve only spans to 1200 GPM, but 150% of rated (1000) is 1500 GPM.
		CandidatePumpCurve candidate = new CandidatePumpCurve(
				Quantities.getQuantity(1000.0, FirePumpUnits.GPM),
				Quantities.getQuantity(100.0, FirePumpUnits.PSI),
				List.of(point(0, 135), point(500, 115), point(1000, 100), point(1200, 90)));
		FirePumpCurveValidationInput input = new FirePumpCurveValidationInput(
				candidate, Quantities.getQuantity(1000.0, FirePumpUnits.GPM), Quantities.getQuantity(100.0, FirePumpUnits.PSI));

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

}
