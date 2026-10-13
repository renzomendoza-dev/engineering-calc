package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.JsonPipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityCalculator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hand-worked cases, including the front page of the Everstrong hydraulic calculation workbook.
 * Uses the shipped reference data (1.2 x sqrt(A), 7 psi), so what ships is what is tested.
 */
class RemoteAreaPlanCalculatorTest {

	private final RemoteAreaRulesResolver rules = new JsonRemoteAreaRulesResolver();
	private final RemoteAreaPlanCalculator calculator = new RemoteAreaPlanCalculator(
			rules, new QuickResponseAreaReductionCalculator(new JsonQuickResponseAreaReductionResolver()));

	private RemoteAreaPlanInput input(
			double area, double density, double k, double coverage, Double spacing,
			boolean quickResponse, Double ceilingFt, Double maxCoverage) {
		return new RemoteAreaPlanInput(area, density, k, coverage, spacing, quickResponse, ceilingFt, maxCoverage);
	}

	@Test
	void workbookFrontPage_ordinaryHazard_matchesTheSheet() {
		// Everstrong workbook, Front Page: ordinary hazard, AS = 130 ft2, D = 0.15, AP = 1500 ft2, K 5.6.
		//   number of sprinklers = ROUNDUP(1500 / 130)   = 12        (11.54 rounds up)
		//   flow per sprinkler   = 0.15 * 130            = 19.5 gpm
		//   P residual           = (19.5 / 5.6)^2        = 12.1253 psi  -- above 7, so it stands
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.15, 5.6, 130, null, false, null, 130.0));

		assertEquals(12, plan.sprinklersForArea());
		assertEquals(19.5, plan.flowPerSprinklerGpm(), 1e-9);
		assertEquals(12.125319, plan.pressureForFlowPsi(), 1e-6);
		assertFalse(plan.floorGoverned());
		assertEquals(12.125319, plan.firstSprinklerPressurePsi(), 1e-6);
		// Nothing was added by a floor, so it discharges exactly the design flow.
		assertEquals(19.5, plan.firstSprinklerFlowGpm(), 1e-9);
	}

	@Test
	void workbookLayout_fourPerBranchOnThreeBranches_withTwelveFootSpacing() {
		// The workbook builds 3 branch lines of 4 sprinklers. With 12 ft along the branch:
		//   length = 1.2 * sqrt(1500) = 46.476 ft;  46.476 / 12 = 3.87 -> 4 per branch
		//   branches = ceil(1500 / (4 * 130)) = ceil(2.88) = 3;  total = 12;  protects 1560 ft2
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.15, 5.6, 130, 12.0, false, null, 130.0));

		assertEquals(4, plan.sprinklersPerBranch());
		assertEquals(3, plan.branchLines());
		assertEquals(12, plan.totalSprinklers());
		assertEquals(1560.0, plan.totalProtectedAreaSqFt(), 1e-9);
		// 130 ft2 over 12 ft along the branch leaves 10.83 ft between branches.
		assertEquals(130.0 / 12.0, plan.branchSpacingFt(), 1e-9);
	}

	@Test
	void withoutTheSpacing_everythingButTheLayoutIsStillWorkedOut() {
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 100, null, false, null, null));

		// Available from the design basis alone, so a form can show it before layout is known.
		assertEquals(15, plan.sprinklersForArea());
		assertEquals(10.0, plan.flowPerSprinklerGpm(), 1e-9);
		assertEquals(46.4758, plan.remoteAreaLengthFt(), 1e-3);
		assertEquals(32.2749, plan.remoteAreaWidthFt(), 1e-3);
		assertEquals(7.0, plan.firstSprinklerPressurePsi(), 1e-9);

		assertFalse(plan.hasLayout());
		assertNull(plan.branchSpacingFt());
		assertNull(plan.sprinklersPerBranch());
		assertNull(plan.branchLines());
		assertNull(plan.totalSprinklers());
		assertNull(plan.totalProtectedAreaSqFt());
	}

	@Test
	void lightHazardOnTenByTen_matchesTheHandWorkedLayoutAndTheFlooredFirstSprinkler() {
		// A = 1500 ft2, density 0.10, K 5.6, 100 ft2 each, 10 ft along the branch.
		//   length  = 1.2 * sqrt(1500) = 46.476 ft;  width = 32.275 ft
		//   per branch = ceil(4.65) = 5;  branches = ceil(1500 / (5 * 100)) = 3;  15 sprinklers
		//   flow = 0.10 * 100 = 10 gpm  ->  (10 / 5.6)^2 = 3.189 psi, BELOW 7 psi
		//   so 7 psi governs, and the sprinkler discharges 5.6 * sqrt(7) = 14.816 gpm
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 100, 10.0, false, null, 225.0));

		assertEquals(1500.0, plan.adjustedDesignAreaSqFt(), 1e-9);
		assertEquals(15, plan.sprinklersForArea());
		assertEquals(5, plan.sprinklersPerBranch());
		assertEquals(3, plan.branchLines());
		assertEquals(15, plan.totalSprinklers());
		assertEquals(1500.0, plan.totalProtectedAreaSqFt(), 1e-9);
		assertEquals(10.0, plan.branchSpacingFt(), 1e-9);

		assertEquals(3.1888, plan.pressureForFlowPsi(), 1e-3);
		assertTrue(plan.floorGoverned());
		assertEquals(7.0, plan.firstSprinklerPressurePsi(), 1e-9);
		assertEquals(14.8162073, plan.firstSprinklerFlowGpm(), 1e-6);
		assertTrue(
				plan.notes().stream().anyMatch(n -> n.contains("below the 7 psi minimum operating pressure")),
				"the floor taking over should be explained: " + plan.notes());
	}

	@Test
	void whenTheFlowNeedsMoreThanSevenPsi_theFloorStaysOutOfIt() {
		// Ordinary Group 2: 0.20 over 130 ft2 = 26 gpm; (26 / 5.6)^2 = 21.556 psi.
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.20, 5.6, 130, null, false, null, 130.0));

		assertFalse(plan.floorGoverned());
		assertEquals(26.0, plan.flowPerSprinklerGpm(), 1e-9);
		assertEquals(21.5561, plan.firstSprinklerPressurePsi(), 1e-3);
		assertEquals(26.0, plan.firstSprinklerFlowGpm(), 1e-9);
		assertTrue(plan.notes().stream().noneMatch(n -> n.contains("minimum operating pressure")));
	}

	@Test
	void quickResponseReduction_feedsTheCountsAndTheLayout() {
		// 15 ft ceiling: -3(15)/2 + 55 = 32.5 percent off 1500 -> 1012.5 ft2.
		//   fewest sprinklers = ceil(1012.5 / 100) = 11
		//   length = 1.2 * sqrt(1012.5) = 38.184 ft;  per branch = ceil(3.818) = 4
		//   branches = ceil(1012.5 / (4 * 100)) = ceil(2.53) = 3;  total = 12;  protects 1200 ft2
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 100, 10.0, true, 15.0, 225.0));

		assertTrue(plan.reductionApplied());
		assertEquals(32.5, plan.reductionPercent(), 1e-9);
		assertEquals(1500.0, plan.baseDesignAreaSqFt(), 1e-9);
		assertEquals(1012.5, plan.adjustedDesignAreaSqFt(), 1e-9);
		assertEquals(11, plan.sprinklersForArea());
		assertEquals(38.1838, plan.remoteAreaLengthFt(), 1e-3);
		assertEquals(4, plan.sprinklersPerBranch());
		assertEquals(3, plan.branchLines());
		assertEquals(12, plan.totalSprinklers());
		assertEquals(1200.0, plan.totalProtectedAreaSqFt(), 1e-9);
	}

	@Test
	void ceilingAboveTwentyFeet_withQuickResponse_takesNoReduction() {
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 100, 10.0, true, 24.0, null));

		assertFalse(plan.reductionApplied());
		assertEquals(1500.0, plan.adjustedDesignAreaSqFt(), 1e-9);
		assertEquals(15, plan.totalSprinklers());
	}

	@Test
	void anExactFit_isNotRoundedUpPastWhatItNeeds() {
		// A = 2500: length = 1.2 * 50 = 60 ft, and 60 / 10 is exactly 6 -- not 7. Float noise in the
		// division must not turn a whole number of sprinklers into one more.
		RemoteAreaPlanResult plan = calculator.calculate(input(2500, 0.30, 5.6, 100, 10.0, false, null, null));

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
						calculator.calculate(input(area, 0.10, 5.6, 100, spacing, false, null, null));
				assertTrue(
						plan.totalProtectedAreaSqFt() >= plan.adjustedDesignAreaSqFt() - 1e-9,
						"area " + area + " spacing " + spacing + " protects " + plan.totalProtectedAreaSqFt());
				assertTrue(
						plan.totalSprinklers() >= plan.sprinklersForArea(),
						"area " + area + " spacing " + spacing + ": " + plan.totalSprinklers()
								+ " < the " + plan.sprinklersForArea() + " the area needs");
			}
		}
	}

	@Test
	void coverageAboveWhatTheHazardAllows_isWarnedAbout() {
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 256, null, false, null, 225.0));

		assertTrue(
				plan.warnings().stream().anyMatch(w -> w.contains("above the 225 ft2 maximum")),
				"expected a coverage warning: " + plan.warnings());
	}

	@Test
	void confirmedRules_areNotFlagged() {
		// The 1.2 x sqrt(A) rule (27.2.4.2.1) and the 7 psi minimum (27.2.4.11.1) have both been
		// checked against the printed standard, so a plan no longer asks anyone to confirm them.
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 100, null, false, null, null));

		assertEquals("verified", plan.rulesConfidence());
		assertTrue(
				plan.warnings().stream().noneMatch(w -> w.contains("reference data")),
				"confirmed rules should not be flagged: " + plan.warnings());
	}

	@Test
	void theLayoutNote_saysTheAreaHasToPermitSprinklersOnBothSidesOfTheCrossMain() {
		RemoteAreaPlanResult plan = calculator.calculate(input(1500, 0.10, 5.6, 100, 10.0, false, null, null));

		assertTrue(
				plan.notes().stream().anyMatch(n -> n.contains("27.2.4.2.1") && n.contains("both sides of the cross main")),
				"expected the rule to be named: " + plan.notes());
	}

	@Test
	void quickResponseWithoutACeilingHeight_isRejected() {
		CalculationException thrown = assertThrows(
				CalculationException.class,
				() -> input(1500, 0.10, 5.6, 100, null, true, null, null));
		assertTrue(thrown.getMessage().contains("ceilingHeightFt is required"), thrown.getMessage());
	}

	@Test
	void nonPositiveFigures_areRejected_andTheSpacingMayBeAbsent() {
		assertThrows(CalculationException.class, () -> input(0, 0.10, 5.6, 100, null, false, null, null));
		assertThrows(CalculationException.class, () -> input(1500, 0, 5.6, 100, null, false, null, null));
		assertThrows(CalculationException.class, () -> input(1500, 0.10, 0, 100, null, false, null, null));
		assertThrows(CalculationException.class, () -> input(1500, 0.10, 5.6, 0, null, false, null, null));
		// A spacing that is given must be sensible; one that is absent is simply not laid out.
		assertThrows(CalculationException.class, () -> input(1500, 0.10, 5.6, 100, 0.0, false, null, null));
		assertThrows(CalculationException.class, () -> input(1500, 0.10, 5.6, 100, -4.0, false, null, null));
		calculator.calculate(input(1500, 0.10, 5.6, 100, null, false, null, null));
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

	@Test
	void aListedMinimumAboveTheStandards_raisesTheFloorForTheFirstSprinkler() {
		// 10 gpm over 100 ft2 needs 3.19 psi, so the floor governs. The standard's floor is 7 psi; a
		// listing that asks for 10 psi (27.2.4.11.2) makes it 10, and the sprinkler discharges more.
		RemoteAreaPlanResult plan = calculator.calculate(new RemoteAreaPlanInput(
				1500, 0.10, 5.6, 100, null, false, null, null, 10.0));

		assertTrue(plan.floorGoverned());
		assertEquals(10.0, plan.minimumOperatingPressurePsi(), 1e-9);
		assertEquals(10.0, plan.firstSprinklerPressurePsi(), 1e-9);
		assertEquals(5.6 * Math.sqrt(10.0), plan.firstSprinklerFlowGpm(), 1e-9);
		assertTrue(
				plan.notes().stream().anyMatch(n -> n.contains("listing's minimum operating pressure of 10 psi")),
				"the listing should be named: " + plan.notes());
	}

	@Test
	void aListedMinimumBelowTheStandards_changesNothing() {
		// The listing can only raise the floor; 5 psi under 7 is ignored, as is no listing at all.
		RemoteAreaPlanResult withListing = calculator.calculate(new RemoteAreaPlanInput(
				1500, 0.10, 5.6, 100, null, false, null, null, 5.0));
		RemoteAreaPlanResult without = calculator.calculate(input(1500, 0.10, 5.6, 100, null, false, null, null));

		assertEquals(7.0, withListing.minimumOperatingPressurePsi(), 1e-9);
		assertEquals(without.firstSprinklerPressurePsi(), withListing.firstSprinklerPressurePsi(), 1e-9);
		assertTrue(withListing.notes().stream().noneMatch(n -> n.contains("listing's minimum")));
	}

	@Test
	void aNegativeListedMinimum_isRejected() {
		assertThrows(
				CalculationException.class,
				() -> new RemoteAreaPlanInput(1500, 0.10, 5.6, 100, null, false, null, null, -1.0));
	}
}
