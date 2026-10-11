package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Anchored to NFPA 13 (2019) Figure 19.3.3.2.3.1 as printed: flat 40 percent below 10 ft;
 * y = -3x/2 + 55 from 10 ft to 20 ft inclusive; none above 20 ft. Uses the real reference file, so
 * what ships is what is tested.
 */
class QuickResponseAreaReductionCalculatorTest {

	private final QuickResponseAreaReductionCalculator calculator =
			new QuickResponseAreaReductionCalculator(new JsonQuickResponseAreaReductionResolver());

	private QuickResponseAreaReductionResult at(double ceilingFt, boolean quickResponse) {
		return calculator.calculate(new QuickResponseAreaReductionInput(1500.0, ceilingFt, quickResponse));
	}

	@Test
	void belowTenFeet_takesTheFlatFortyPercent() {
		QuickResponseAreaReductionResult result = at(8.0, true);

		assertEquals(40.0, result.reductionPercent(), 1e-9);
		assertEquals(900.0, result.adjustedDesignAreaSqFt(), 1e-9);
		assertTrue(result.reductionApplied());
	}

	@Test
	void atTenFeet_theLineAndTheFlatPartMeetAtForty() {
		// y = -3(10)/2 + 55 = 40 -- the same value the flat part gives just below it, so no jump.
		assertEquals(40.0, at(10.0, true).reductionPercent(), 1e-9);
		assertEquals(40.0, at(9.999, true).reductionPercent(), 1e-9);
	}

	@Test
	void betweenTenAndTwentyFeet_followsThePrintedLine() {
		// 12 ft: -18 + 55 = 37.  15 ft: -22.5 + 55 = 32.5.  18 ft: -27 + 55 = 28.
		assertEquals(37.0, at(12.0, true).reductionPercent(), 1e-9);
		assertEquals(32.5, at(15.0, true).reductionPercent(), 1e-9);
		assertEquals(28.0, at(18.0, true).reductionPercent(), 1e-9);

		// 15 ft on a 1500 ft2 area: 1500 * (1 - 0.325) = 1012.5 ft2.
		assertEquals(1012.5, at(15.0, true).adjustedDesignAreaSqFt(), 1e-9);
	}

	@Test
	void atTwentyFeet_isInclusive_andTakesTwentyFivePercent() {
		// The figure says ">= 10 ft and <= 20 ft" for the line, so 20.0 is on it.
		QuickResponseAreaReductionResult result = at(20.0, true);

		assertEquals(25.0, result.reductionPercent(), 1e-9);
		assertEquals(1125.0, result.adjustedDesignAreaSqFt(), 1e-9);
	}

	@Test
	void justAboveTwentyFeet_dropsToNothing_thatStepIsTheStandards() {
		QuickResponseAreaReductionResult result = at(20.01, true);

		assertEquals(0.0, result.reductionPercent(), 1e-9);
		assertEquals(1500.0, result.adjustedDesignAreaSqFt(), 1e-9);
		assertFalse(result.reductionApplied());
		assertTrue(
				result.notes().stream().anyMatch(n -> n.contains("above the 20 ft")),
				"the reason should be stated: " + result.notes());
	}

	@Test
	void withoutQuickResponseSprinklers_noReductionAtAnyHeight() {
		for (double ceiling : new double[] {8.0, 12.0, 20.0}) {
			QuickResponseAreaReductionResult result = at(ceiling, false);
			assertEquals(0.0, result.reductionPercent(), 0.0, "ceiling " + ceiling);
			assertEquals(1500.0, result.adjustedDesignAreaSqFt(), 0.0, "ceiling " + ceiling);
			assertFalse(result.reductionApplied());
		}
	}

	@Test
	void appliedReduction_alwaysSaysTheClausesConditionsWereNotChecked() {
		// A reduction is the less conservative direction, so this is not allowed to go missing.
		QuickResponseAreaReductionResult result = at(12.0, true);

		assertTrue(result.reductionApplied());
		assertTrue(
				result.notes().stream().anyMatch(n -> n.contains("not checked here")),
				"an applied reduction must disclose the unchecked conditions: " + result.notes());
		assertTrue(result.source().contains("19.3.3.2.3.1"), result.source());
		assertEquals("verified", result.confidence());
	}

	@Test
	void notesCarryNoDisclosureWhenNothingWasReduced() {
		// Nothing was taken off, so there is no less-conservative result to caveat.
		QuickResponseAreaReductionResult result = at(25.0, true);

		assertTrue(result.notes().stream().noneMatch(n -> n.contains("not checked here")), result.notes().toString());
	}

	@Test
	void curveMatchesThePrintedSiNote_atItsEndPoints() {
		// The figure's SI note is y = -4.8x + 54.6 with x in metres; at 3.0 m and 6.1 m (10 and 20
		// ft) it should land on the US figures, within the rounding of the metric conversion.
		double ftToM = 0.3048;
		double siAt10 = -4.8 * (10 * ftToM) + 54.6;
		double siAt20 = -4.8 * (20 * ftToM) + 54.6;

		assertEquals(at(10.0, true).reductionPercent(), siAt10, 0.5);
		assertEquals(at(20.0, true).reductionPercent(), siAt20, 0.5);
	}

	@Test
	void nonPositiveInputs_areRejected() {
		assertThrows(CalculationException.class, () -> new QuickResponseAreaReductionInput(0.0, 12.0, true));
		assertThrows(CalculationException.class, () -> new QuickResponseAreaReductionInput(1500.0, 0.0, true));
		assertThrows(CalculationException.class, () -> new QuickResponseAreaReductionInput(1500.0, -3.0, true));
	}

}
