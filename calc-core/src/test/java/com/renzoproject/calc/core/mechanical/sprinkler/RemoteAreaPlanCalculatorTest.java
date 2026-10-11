package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.JsonPipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityCalculator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hand-worked cases. Uses the shipped reference data (1.2 x sqrt(A), 7 psi), so what ships is what
 * is tested.
 */
class RemoteAreaPlanCalculatorTest {

	private final RemoteAreaRulesResolver rules = new JsonRemoteAreaRulesResolver();
	private final RemoteAreaPlanCalculator calculator = new RemoteAreaPlanCalculator(
			rules, new QuickResponseAreaReductionCalculator(new JsonQuickResponseAreaReductionResolver()));

	private RemoteAreaPlanInput input(
			double area, double density, double k, double spacing, double branchSpacing,
			boolean quickResponse, Double ceilingFt, Double maxCoverage) {
		return new RemoteAreaPlanInput(area, density, k, spacing, branchSpacing, quickResponse, ceilingFt, maxCoverage);
	}

	@Test
	void lightHazardOnTenByTenSpacing_matchesTheHandWorkedLayout() {
		// A = 1500 ft2, density 0.10, K 5.6, 10 ft x 10 ft spacing.
		//   length  = 1.2 * sqrt(1500) = 1.2 * 38.7298 = 46.476 ft
		//   width   = 1500 / 46.476    = 32.275 ft
		//   per branch = ceil(46.476 / 10) = ceil(4.65) = 5
		//   branches   = ceil(1500 / (5 * 100)) = ceil(3.0) = 3
		//   total      = 15 sprinklers, protecting 15 * 100 = 1500 ft2
		//   min flow   = 0.10 * 100 = 10 gpm  ->  (10 / 5.6)^2 = 3.189 psi, BELOW 7 psi
		//   so 7 psi governs, and the sprinkler discharges 5.6 * sqrt(7) = 14.816 gpm
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 10, 10, false, null, 225.0));

		assertEquals(1500.0, plan.adjustedDesignAreaSqFt(), 1e-9);
		assertEquals(46.4758, plan.remoteAreaLengthFt(), 1e-3);
		assertEquals(32.2749, plan.remoteAreaWidthFt(), 1e-3);
		assertEquals(100.0, plan.coveragePerSprinklerSqFt(), 1e-9);
		assertEquals(5, plan.sprinklersPerBranch());
		assertEquals(3, plan.branchLines());
		assertEquals(15, plan.totalSprinklers());
		assertEquals(1500.0, plan.totalProtectedAreaSqFt(), 1e-9);

		assertEquals(10.0, plan.minimumFlowPerSprinklerGpm(), 1e-9);
		assertEquals(3.1888, plan.pressureForMinimumFlowPsi(), 1e-3);
		assertTrue(plan.floorGoverned());
		assertEquals(7.0, plan.firstSprinklerPressurePsi(), 1e-9);
		assertEquals(14.8162073, plan.firstSprinklerFlowGpm(), 1e-6);
		assertTrue(
				plan.notes().stream().anyMatch(n -> n.contains("below the 7 psi minimum operating pressure")),
				"the floor taking over should be explained: " + plan.notes());
	}

	@Test
	void whenTheDemandNeedsMoreThanSevenPsi_theFloorStaysOutOfIt() {
		// Ordinary Group 2: density 0.20 over 13 x 10 = 130 ft2 = 26 gpm; (26 / 5.6)^2 = 21.556 psi.
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.20, 5.6, 13, 10, false, null, 130.0));

		assertFalse(plan.floorGoverned());
		assertEquals(26.0, plan.minimumFlowPerSprinklerGpm(), 1e-9);
		assertEquals(21.5561, plan.firstSprinklerPressurePsi(), 1e-3);
		// Delivered flow is exactly the minimum -- nothing extra was added.
		assertEquals(26.0, plan.firstSprinklerFlowGpm(), 1e-9);
		assertTrue(plan.notes().stream().noneMatch(n -> n.contains("minimum operating pressure")));
	}

	@Test
	void quickResponseReduction_feedsTheLayout() {
		// 15 ft ceiling: -3(15)/2 + 55 = 32.5 percent off 1500 -> 1012.5 ft2.
		//   length = 1.2 * sqrt(1012.5) = 38.184 ft;  per branch = ceil(3.818) = 4
		//   branches = ceil(1012.5 / (4 * 100)) = ceil(2.53) = 3;  total = 12;  protects 1200 ft2
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 10, 10, true, 15.0, 225.0));

		assertTrue(plan.reductionApplied());
		assertEquals(32.5, plan.reductionPercent(), 1e-9);
		assertEquals(1500.0, plan.baseDesignAreaSqFt(), 1e-9);
		assertEquals(1012.5, plan.adjustedDesignAreaSqFt(), 1e-9);
		assertEquals(38.1838, plan.remoteAreaLengthFt(), 1e-3);
		assertEquals(4, plan.sprinklersPerBranch());
		assertEquals(3, plan.branchLines());
		assertEquals(12, plan.totalSprinklers());
		assertEquals(1200.0, plan.totalProtectedAreaSqFt(), 1e-9);
	}

	@Test
	void ceilingAboveTwentyFeet_withQuickResponse_takesNoReduction() {
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 10, 10, true, 24.0, null));

		assertFalse(plan.reductionApplied());
		assertEquals(1500.0, plan.adjustedDesignAreaSqFt(), 1e-9);
		assertEquals(15, plan.totalSprinklers());
	}

	@Test
	void anExactFit_isNotRoundedUpAPastWhatItNeeds() {
		// A = 2500: length = 1.2 * 50 = 60 ft, and 60 / 10 is exactly 6 -- not 7. Float noise in the
		// division must not turn a whole number of sprinklers into one more.
		RemoteAreaPlanResult plan = calculator.calculate(input(2500, 0.30, 5.6, 10, 10, false, null, null));

		assertEquals(6, plan.sprinklersPerBranch());
		// 2500 / (6 * 100) = 4.17 -> 5 branches, 30 sprinklers, 3000 ft2.
		assertEquals(5, plan.branchLines());
		assertEquals(30, plan.totalSprinklers());
		assertEquals(3000.0, plan.totalProtectedAreaSqFt(), 1e-9);
	}

	@Test
	void theCountsRoundUp_soThePlanIsNeverSmallerThanTheDesignArea() {
		for (double area : new double[] {700, 1000, 1234, 1500, 1950, 2500, 3250}) {
			for (double spacing : new double[] {8, 10, 12, 13}) {
				RemoteAreaPlanResult plan =
						calculator.calculate(input(area, 0.10, 5.6, spacing, 10, false, null, null));
				assertTrue(
						plan.totalProtectedAreaSqFt() >= plan.adjustedDesignAreaSqFt() - 1e-9,
						"area " + area + " spacing " + spacing + " protects " + plan.totalProtectedAreaSqFt());
			}
		}
	}

	@Test
	void spacingCoveringMoreThanTheHazardAllows_isWarnedAbout() {
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 16, 16, false, null, 225.0));

		assertEquals(256.0, plan.coveragePerSprinklerSqFt(), 1e-9);
		assertTrue(
				plan.warnings().stream().anyMatch(w -> w.contains("above the 225 ft2 maximum")),
				"expected a coverage warning: " + plan.warnings());
	}

	@Test
	void unverifiedRules_areDisclosedOnEveryPlan() {
		// remote-area-rules.json is tagged 'verify' until the 1.2 x sqrt(A) rule and the 7 psi minimum
		// are checked against the printed standard; the plan must say so each time.
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 10, 10, false, null, null));

		assertEquals("verify", plan.rulesConfidence());
		assertTrue(
				plan.warnings().stream().anyMatch(w -> w.contains("confirm both against the printed NFPA 13")),
				"unverified rules should be flagged: " + plan.warnings());
	}

	@Test
	void quickResponseWithoutACeilingHeight_isRejected() {
		CalculationException thrown = assertThrows(
				CalculationException.class,
				() -> input(1500, 0.10, 5.6, 10, 10, true, null, null));
		assertTrue(thrown.getMessage().contains("ceilingHeightFt is required"), thrown.getMessage());
	}

	@Test
	void nonPositiveFigures_areRejected() {
		assertThrows(CalculationException.class, () -> input(0, 0.10, 5.6, 10, 10, false, null, null));
		assertThrows(CalculationException.class, () -> input(1500, 0, 5.6, 10, 10, false, null, null));
		assertThrows(CalculationException.class, () -> input(1500, 0.10, 0, 10, 10, false, null, null));
		assertThrows(CalculationException.class, () -> input(1500, 0.10, 5.6, 0, 10, false, null, null));
		assertThrows(CalculationException.class, () -> input(1500, 0.10, 5.6, 10, -2, false, null, null));
	}

	// --- the same floor, enforced in the hydraulic calculation itself ---

	private final JsonPipeDimensionResolver dimensionResolver = new JsonPipeDimensionResolver();
	private final SprinklerHydraulicCalculator hydraulics = new SprinklerHydraulicCalculator(
			dimensionResolver,
			new JsonFittingEquivalentLengthResolver(),
			new PipeVelocityCalculator(dimensionResolver));

	private double firstSprinklerFlow(double density, double coverage, double assumedStartPsi) {
		SprinklerHydraulicResult result = hydraulics.calculate(new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, density, 1500.0, 0.0, null, assumedStartPsi,
				List.of(new RemoteAreaSprinkler("S1", 5.6, coverage)),
				List.of(new SprinklerPipeSegment("S1", "RISER", "BI", "SCH40", "1", 120.0, 10.0, 0.0, List.of())),
				new SupplyCurve(80.0, 60.0, 1000.0),
				List.of()));
		return result.segments().get(0).flowGpm();
	}

	@Test
	void hydraulicCalculation_neverStartsBelowTheFloor_whateverTheCallerAssumes() {
		// 0.05 gpm/ft2 over 130 ft2 = 6.5 gpm, which needs only 1.35 psi. Asking for an assumed start
		// of 0 ("let the minimum flow govern outright") used to start there; it now starts at 7 psi.
		double flowAtFloor = 5.6 * Math.sqrt(7.0);

		assertEquals(flowAtFloor, firstSprinklerFlow(0.05, 130.0, 0.0), 1e-9);
		assertEquals(flowAtFloor, firstSprinklerFlow(0.05, 130.0, 3.0), 1e-9);
		assertEquals(flowAtFloor, firstSprinklerFlow(0.05, 130.0, 7.0), 1e-9);
	}

	@Test
	void hydraulicCalculation_stillLetsTheCallerRaiseTheStartPressure() {
		// The assumed figure can lift the start above the floor; it just cannot lower it.
		assertEquals(5.6 * Math.sqrt(10.0), firstSprinklerFlow(0.05, 130.0, 10.0), 1e-9);
	}

}
