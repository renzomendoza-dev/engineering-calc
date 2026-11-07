package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.JsonPipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.standpipe.StandpipeHydraulicResult.SegmentRow;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandpipeHydraulicCalculatorTest {

	private final StandpipeHydraulicCalculator calculator =
			new StandpipeHydraulicCalculator(new JsonPipeDimensionResolver(), new JsonStandpipeReferenceResolver());

	private static StandpipeSegment hosePiping(String label, double flow, HoseConnectionSize size, String nominal, double length, double rise,
			List<StandpipeFitting> fittings) {
		return new StandpipeSegment(label, PipeRole.HOSE_PIPING, flow, size, "BI", "SCH40", nominal, 120, length, rise, fittings);
	}

	private static StandpipeSegment riser(String label, double flow, String nominal, double length, double rise, List<StandpipeFitting> fittings) {
		return new StandpipeSegment(label, PipeRole.STANDPIPE, flow, null, "BI", "SCH40", nominal, 120, length, rise, fittings);
	}

	private static StandpipeHydraulicInput input(StandpipeClass standpipeClass, SprinklerProtection protection, List<StandpipeSegment> segments) {
		return new StandpipeHydraulicInput(standpipeClass, protection, null, null, 0, false, false, 0, false, false, null, null, null, null, segments);
	}

	/** The Carmona hydraulic workbook, sheet "BROILER FARM" (the one the report quotes): a Class II 1.5 in. hose station. */
	private StandpipeHydraulicResult carmona(double durationMinutes) {
		List<StandpipeFitting> riserFittings = List.of(
				new StandpipeFitting("ELBOW_90_STANDARD", 2, null),
				new StandpipeFitting("GATE_VALVE", 1, null),
				new StandpipeFitting("SWING_CHECK_VALVE", 1, null));
		List<StandpipeSegment> segments = List.of(
				hosePiping("A", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 30, 0, null),
				riser("Riser", 0, "3", 400, 5, riserFittings));
		return calculator.calculate(new StandpipeHydraulicInput(StandpipeClass.CLASS_II, SprinklerProtection.NONE, null, null, 0, false, false, 0, false, false, null,
				null, durationMinutes, 50.0, segments));
	}

	@Test
	void carmonaWorkbook_pipeAndRiser_matchTheSheetWithinTheDiameterItUsed() {
		var result = carmona(60.0);

		SegmentRow a = result.segments().get(0);
		SegmentRow riser = result.segments().get(1);
		// The sheet's 1.5 in. line: 30 ft of 1.61 in. pipe at 100 gpm, C 120, is 9.52 psi, on top of the 65 psi residual.
		assertEquals(1.61, a.insideDiameterInch(), 0.005);
		// The catalog bore is 1.612 in., so the loss comes out 0.5 percent lower than the sheet's.
		assertEquals(9.52, a.frictionLossPsi(), 0.08);
		assertEquals(65.0, a.pressureAtRemoteEndPsi(), 1e-9);
		assertEquals(74.52, a.pressureAtSupplyEndPsi(), 0.08);
		// The riser: 2 elbows (7 ft each), 1 gate valve (1 ft) and 1 swing check (16 ft) at 3 in. is 31 ft, as the sheet's I6.
		assertEquals(31.0, riser.fittingsLengthFt(), 1e-9);
		assertEquals(431.0, riser.pipeLengthFt() + riser.fittingsLengthFt(), 1e-9);
		assertEquals(5 * 0.433, riser.elevationPsi(), 1e-9);
		// The sheet used 3.05 in. for the 3 in. riser and got 6.09 psi; the pipe catalog's Schedule 40 bore is 3.07 in.
		assertEquals(3.07, riser.insideDiameterInch(), 0.005);
		assertEquals(6.09, riser.frictionLossPsi(), 0.2);
		assertEquals(82.77, result.pressureAtSupplyPsi(), 0.3);
	}

	@Test
	void carmonaWorkbook_flowsAndVolume() {
		var result = carmona(60.0);

		assertEquals(100.0, result.segments().get(1).flowGpm(), 1e-9);
		assertEquals(150.0, result.flowAtSupplyGpm(), 1e-9);
		// The report's 150 gpm for 60 minutes is 9,000 gal.
		assertEquals(9_000.0, result.waterSupplyVolumeGal(), 1e-6);
		assertEquals(100.0, result.requiredFlowGpm(), 1e-9);
		assertEquals(1, result.requiredFdcInlets());
	}

	@Test
	void riserVelocity_isFlowOverBoreArea() {
		// The report works 150 gpm in a 3 in. pipe as 6.81 ft/s with r = 1.5 in., and its printed 10.21 ft/s is that at 150 percent.
		// With the catalog bore of 3.07 in. it is 6.51 ft/s.
		var segments = List.of(
				hosePiping("A", 150, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 1, 0, null),
				riser("Riser", 0, "3", 10, 0, null));

		var result = calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, segments));

		double bore = result.segments().get(1).insideDiameterInch();
		assertEquals(150.0 / 448.831 / (Math.PI * bore * bore / 4 / 144), result.segments().get(1).velocityFtPerSec(), 0.01);
		assertEquals(6.51, result.segments().get(1).velocityFtPerSec(), 0.05);
	}

	@Test
	void fittingLength_isRescaledForCAndExplicitLengthIsNot() {
		StandpipeSegment atC120 = riser("a", 100, "3", 10, 0, List.of(new StandpipeFitting("TEE_OR_CROSS", 2, null)));
		StandpipeSegment atC100 = new StandpipeSegment("b", PipeRole.STANDPIPE, 100.0, null, "BI", "SCH40", "3", 100, 10, 0,
				List.of(new StandpipeFitting("TEE_OR_CROSS", 2, null)));
		StandpipeSegment explicit = new StandpipeSegment("c", PipeRole.STANDPIPE, 100.0, null, "BI", "SCH40", "3", 100, 10, 0,
				List.of(new StandpipeFitting(null, 2, 7.5)));
		var hose = hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 1, 0, null);

		var result = calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, List.of(hose, atC120, atC100, explicit)));

		assertEquals(30.0, result.segments().get(1).fittingsLengthFt(), 1e-9);
		// Table 8.3.2.2: x 0.713 at C = 100.
		assertEquals(30.0 * 0.713, result.segments().get(2).fittingsLengthFt(), 0.05);
		assertEquals(15.0, result.segments().get(3).fittingsLengthFt(), 1e-9);
	}

	@Test
	void hoseValveFriction_comesFromTheChart() {
		var hose = hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0,
				List.of(new StandpipeFitting("ANGLE_HOSE_VALVE", 1, null)));

		var result = calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, List.of(hose)));

		assertEquals(20.0, result.segments().get(0).fittingsLengthFt(), 1e-9);
	}

	@Test
	void fittingWithNoPublishedLengthForTheSize_isRejected() {
		var hose = hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "2", 5, 0, List.of(new StandpipeFitting("ANGLE_HOSE_VALVE", 1, null)));

		assertThrows(CalculationException.class, () -> calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, List.of(hose))));
	}

	@Test
	void secondConnection_isRaisedToItsMinimumResidualWhenThePathGivesLess() {
		// Two 2.5 in. connections at 250 gpm.
		var top = hosePiping("Top connection", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 2, 0, null);
		// The second connection is 20 ft higher than the first, so the path loses pressure getting there.
		var drop = new StandpipeSegment("Riser top", PipeRole.STANDPIPE, 0.0, null, "BI", "SCH40", "4", 120, 3, -20, null);
		var second = hosePiping("Second connection", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 2, 0, null);
		var riser = riser("Riser", 0, "4", 50, 30, null);

		var result = calculator.calculate(input(StandpipeClass.CLASS_I, SprinklerProtection.NONE, List.of(top, drop, second, riser)));

		SegmentRow secondRow = result.segments().get(2);
		assertTrue(secondRow.residualRaised());
		assertEquals(100.0, secondRow.pressureAtRemoteEndPsi(), 1e-9);
		assertFalse(result.segments().get(0).residualRaised());
		assertEquals(500.0, result.segments().get(3).flowGpm(), 1e-9);
	}

	@Test
	void pressureAccumulatesDownThePath() {
		var hose = hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 30, 0, null);
		var riser = riser("r", 0, "3", 100, 20, null);

		var result = calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, List.of(hose, riser)));

		double expected = 65 + result.totalFrictionLossPsi() + 20 * 0.433;
		assertEquals(expected, result.pressureAtSupplyPsi(), 1e-9);
		assertEquals(20 * 0.433, result.totalElevationPsi(), 1e-9);
	}

	private StandpipeHydraulicResult demand(StandpipeClass standpipeClass, SprinklerProtection protection, CombinedHazard hazard, Double sprinkler,
			int additional, boolean over80k, boolean horizontal) {
		var segments = List.of(hosePiping("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null));
		return calculator.calculate(new StandpipeHydraulicInput(standpipeClass, protection, hazard, sprinkler, additional, over80k, horizontal, 0, false, false, null,
				null, null, null, segments));
	}

	@Test
	void demand_classIAndIII_firstStandpipeAndAdditionalStandpipes() {
		assertEquals(500.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 0, false, false).requiredFlowGpm(), 1e-9);
		assertEquals(500.0, demand(StandpipeClass.CLASS_III, SprinklerProtection.NONE, null, null, 0, false, false).requiredFlowGpm(), 1e-9);
		assertEquals(750.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 0, false, true).requiredFlowGpm(), 1e-9);
		assertEquals(750.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 1, false, false).requiredFlowGpm(), 1e-9);
		assertEquals(1000.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 2, false, false).requiredFlowGpm(), 1e-9);
	}

	@Test
	void demand_largeFloors_secondStandpipe500AndThirdOnlyIfUnsprinklered() {
		assertEquals(1000.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 1, true, false).requiredFlowGpm(), 1e-9);
		assertEquals(1250.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 2, true, false).requiredFlowGpm(), 1e-9);
		var sprinklered = demand(StandpipeClass.CLASS_I, SprinklerProtection.PARTIAL, CombinedHazard.LIGHT, null, 2, true, false);
		assertEquals(1000.0, sprinklered.requiredFlowGpm(), 1e-9);
		assertTrue(sprinklered.checks().stream().anyMatch(c -> c.clause().equals("10.6.1.1.4.1") && c.status() == CheckStatus.CONFIRM));
	}

	@Test
	void demand_isLimitedToTheMaximumForTheBuilding() {
		var sprinklered = demand(StandpipeClass.CLASS_I, SprinklerProtection.FULL, null, null, 3, false, true);
		var unsprinklered = demand(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 3, false, true);

		assertEquals(1000.0, sprinklered.requiredFlowGpm(), 1e-9);
		assertTrue(sprinklered.requiredFlowCapped());
		assertEquals(1250.0, unsprinklered.requiredFlowGpm(), 1e-9);
	}

	@Test
	void demand_classII_isOneHundredAndAdditionalStandpipesAddNothing() {
		var result = demand(StandpipeClass.CLASS_II, SprinklerProtection.NONE, null, null, 3, false, false);

		assertEquals(100.0, result.requiredFlowGpm(), 1e-9);
		assertEquals(100.0, result.mostRemoteStandpipeFlowGpm(), 1e-9);
	}

	@Test
	void demand_fullySprinklered_takesTheGreaterOfTheTwo() {
		assertEquals(650.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.FULL, null, 650.0, 0, false, false).requiredFlowGpm(), 1e-9);
		assertEquals(500.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.FULL, null, 300.0, 0, false, false).requiredFlowGpm(), 1e-9);
	}

	@Test
	void demand_partlySprinklered_addsTheLesserOfTheSprinklerDemandAndTheCap() {
		assertEquals(650.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.PARTIAL, CombinedHazard.LIGHT, 800.0, 0, false, false).requiredFlowGpm(), 1e-9);
		assertEquals(600.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.PARTIAL, CombinedHazard.LIGHT, 100.0, 0, false, false).requiredFlowGpm(), 1e-9);
		assertEquals(1000.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.PARTIAL, CombinedHazard.ORDINARY, 900.0, 0, false, false).requiredFlowGpm(), 1e-9);
		assertThrows(CalculationException.class,
				() -> demand(StandpipeClass.CLASS_I, SprinklerProtection.PARTIAL, null, 100.0, 0, false, false));
	}

	@Test
	void sprinklerDemand_isIgnoredWhenThereAreNoSprinklers() {
		assertEquals(500.0, demand(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, 900.0, 0, false, false).requiredFlowGpm(), 1e-9);
	}

	@Test
	void flowChecks_theSupplyMustCoverTheDemand() {
		var segments = List.of(hosePiping("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null),
				hosePiping("h2", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null),
				riser("r", 0, "4", 20, 10, null));
		var short_ = calculator.calculate(input(StandpipeClass.CLASS_I, SprinklerProtection.NONE, segments));
		var withExtra = calculator.calculate(new StandpipeHydraulicInput(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 1, false, false, 0, false, false, null,
				null, null, 250.0, segments));

		assertEquals(CheckStatus.PASS, status(short_, "10.6.1.1"));
		assertEquals(CheckStatus.PASS, status(short_, "10.6"));
		assertEquals(500.0, short_.flowAtSupplyGpm(), 1e-9);
		assertEquals(750.0, withExtra.flowAtSupplyGpm(), 1e-9);
		assertEquals(750.0, withExtra.requiredFlowGpm(), 1e-9);

		var oneConnection = calculator.calculate(input(StandpipeClass.CLASS_I, SprinklerProtection.NONE,
				List.of(hosePiping("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null))));
		assertEquals(CheckStatus.FAIL, status(oneConnection, "10.6.1.1"));
		// The shortfall is filled in from the demand rules, so the supply check passes: the path itself is what fails.
		assertEquals(CheckStatus.PASS, status(oneConnection, "10.6"));
		assertEquals(250.0, oneConnection.flowAddedAtSupplyGpm(), 1e-9);
	}

	@Test
	void flowAddedAtSupply_isWorkedOutForClassIAndIIIWhenNotGiven() {
		var segments = List.of(hosePiping("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null),
				hosePiping("h2", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null), riser("r", 0, "4", 20, 10, null));
		var twoAdditional = calculator.calculate(new StandpipeHydraulicInput(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 2, false,
				false, 0, false, false, null, null, null, null, segments));

		// 500 on the path, 1,000 required: 500 joins at the supply.
		assertTrue(twoAdditional.flowAddedAtSupplyAutomatic());
		assertEquals(500.0, twoAdditional.flowAddedAtSupplyGpm(), 1e-9);
		assertEquals(1000.0, twoAdditional.flowAtSupplyGpm(), 1e-9);
		assertEquals(1000.0, twoAdditional.requiredFlowGpm(), 1e-9);
		assertEquals(4, twoAdditional.requiredFdcInlets());

		// Nothing is added when the path already carries the demand.
		var none = calculator.calculate(input(StandpipeClass.CLASS_I, SprinklerProtection.NONE, segments));
		assertTrue(none.flowAddedAtSupplyAutomatic());
		assertEquals(0.0, none.flowAddedAtSupplyGpm(), 1e-9);
		assertEquals(500.0, none.flowAtSupplyGpm(), 1e-9);

		// A sprinkler allowance counts too: partly sprinklered, light hazard, 800 gpm of sprinklers adds 150.
		var combined = calculator.calculate(new StandpipeHydraulicInput(StandpipeClass.CLASS_I, SprinklerProtection.PARTIAL, CombinedHazard.LIGHT, 800.0,
				0, false, false, 0, false, false, null, null, null, null, segments));
		assertEquals(150.0, combined.flowAddedAtSupplyGpm(), 1e-9);
		assertEquals(650.0, combined.flowAtSupplyGpm(), 1e-9);
	}

	@Test
	void flowAddedAtSupply_aGivenFigureIsUsedAsItIsAndClassIINeverAssumesOne() {
		var segments = List.of(hosePiping("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null),
				hosePiping("h2", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null), riser("r", 0, "4", 20, 10, null));
		var low = calculator.calculate(new StandpipeHydraulicInput(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 1, false, false, 0, false, false, null,
				null, null, 100.0, segments));

		assertFalse(low.flowAddedAtSupplyAutomatic());
		assertEquals(100.0, low.flowAddedAtSupplyGpm(), 1e-9);
		assertEquals(CheckStatus.FAIL, status(low, "10.6"));

		var classII = carmona(60.0);
		assertFalse(classII.flowAddedAtSupplyAutomatic());
		assertEquals(50.0, classII.flowAddedAtSupplyGpm(), 1e-9);
		var classIIBlank = calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE,
				List.of(hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null), riser("r", 0, "3", 10, 0, null))));
		assertFalse(classIIBlank.flowAddedAtSupplyAutomatic());
		assertEquals(0.0, classIIBlank.flowAddedAtSupplyGpm(), 1e-9);
	}

	@Test
	void connectionChecks_sizeFlowAndPressureRegulation() {
		var tooMuch = hosePiping("h", 300, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null);
		assertEquals(CheckStatus.FAIL, status(calculator.calculate(input(StandpipeClass.CLASS_I, SprinklerProtection.NONE, List.of(tooMuch))), "10.6.3"));

		var small = hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null);
		assertEquals(CheckStatus.FAIL, status(calculator.calculate(input(StandpipeClass.CLASS_I, SprinklerProtection.NONE, List.of(small))), "8.2.1"));

		// 1.5 in. connection at 100 gpm on a long line: the residual is the minimum, but raised pressure above 100 psi needs a regulator.
		var first = hosePiping("one", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null);
		var rise = new StandpipeSegment("rise", PipeRole.STANDPIPE, 0.0, null, "BI", "SCH40", "4", 120, 500, 100, null);
		var second = hosePiping("two", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null);
		var out = calculator.calculate(input(StandpipeClass.CLASS_III, SprinklerProtection.NONE, List.of(first, rise, second)));
		assertEquals(CheckStatus.CONFIRM, status(out, "10.2.4.1"));
	}

	@Test
	void minimumPipeSizes_applyToClassIAndIIIOnly() {
		var hose = hosePiping("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null);
		var thin = riser("Riser", 0, "3", 20, 10, null);
		var branch = new StandpipeSegment("Branch", PipeRole.BRANCH_LINE, 0.0, null, "BI", "SCH40", "2", 120, 10, 0, null);
		var six = riser("Big", 0, "4", 20, 10, null);

		var classI = calculator.calculate(input(StandpipeClass.CLASS_I, SprinklerProtection.NONE, List.of(hose, thin, branch)));
		assertEquals(CheckStatus.FAIL, status(classI, "10.3.1.1"));
		assertEquals(CheckStatus.FAIL, status(classI, "10.3.1.4"));

		var partial = calculator.calculate(input(StandpipeClass.CLASS_I, SprinklerProtection.PARTIAL, List.of(hose, six)));
		assertEquals(CheckStatus.FAIL, status(partial, "10.3.1.2"));
		var ok = calculator.calculate(input(StandpipeClass.CLASS_I, SprinklerProtection.NONE, List.of(hose, six)));
		assertEquals(CheckStatus.PASS, status(ok, "10.3.1.1"));

		var classII = calculator.calculate(input(StandpipeClass.CLASS_II,
				SprinklerProtection.NONE, List.of(hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null), thin)));
		assertTrue(classII.checks().stream().noneMatch(c -> c.clause().startsWith("10.3")));
	}

	@Test
	void fireDepartmentConnection_oneInletPerTwoHundredFiftyGpm() {
		var segments = List.of(hosePiping("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null),
				hosePiping("h2", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null), riser("r", 0, "4", 10, 10, null));
		var base = new StandpipeHydraulicInput(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 0, false, false, 0, false, false, null, 1, null, 50.0, segments);

		var result = calculator.calculate(base);

		assertEquals(3, result.requiredFdcInlets());
		assertEquals(CheckStatus.FAIL, status(result, "10.7.3"));
	}

	@Test
	void durationAndPressureLimits() {
		var segments = List.of(hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null), riser("r", 0, "3", 10, 10, null));
		var shortSupply = calculator.calculate(new StandpipeHydraulicInput(StandpipeClass.CLASS_II, SprinklerProtection.NONE, null, null, 0, false, false, 0, false, false, null,
				null, 20.0, null, segments));
		assertEquals(CheckStatus.FAIL, status(shortSupply, "5.2"));
		assertEquals(CheckStatus.PASS, status(calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, segments)), "5.2"));

		var tall = List.of(hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null), riser("r", 0, "3", 10, 900, null));
		assertEquals(CheckStatus.FAIL, status(calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, tall)), "10.2.1"));
		assertEquals(CheckStatus.OPEN, status(shortSupply, "10.2.4.2"));
	}

	@Test
	void nominalSize_isReadAsInches() {
		assertEquals(2.5, StandpipeHydraulicCalculator.nominalInches("2-1/2"), 1e-9);
		assertEquals(4.0, StandpipeHydraulicCalculator.nominalInches("4"), 1e-9);
		assertEquals(0.75, StandpipeHydraulicCalculator.nominalInches("3/4"), 1e-9);
		assertThrows(CalculationException.class, () -> StandpipeHydraulicCalculator.nominalInches("big"));
	}

	@Test
	void inputRules() {
		var hose = hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null);
		var plain = riser("r", 0, "3", 10, 0, null);

		assertThrows(CalculationException.class, () -> input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, List.of()));
		assertThrows(CalculationException.class, () -> input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, List.of(plain)));
		assertThrows(CalculationException.class, () -> input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, List.of(hose, hose)));
		assertThrows(CalculationException.class, () -> new StandpipeSegment("x", PipeRole.OTHER, null, HoseConnectionSize.ONE_AND_HALF_INCH, "BI",
				"SCH40", "3", 120, 1, 0, null));
		assertThrows(CalculationException.class, () -> new StandpipeSegment("x", PipeRole.OTHER, null, null, "BI", "SCH40", "3", 0, 1, 0, null));
		assertThrows(CalculationException.class, () -> new StandpipeFitting(null, 1, null));
		assertNull(new StandpipeSegment("x", PipeRole.OTHER, null, null, "BI", "SCH40", "3", 120, 1, 0, null).hoseConnectionSize());
		// A segment with no flow anywhere upstream carries nothing.
		assertThrows(CalculationException.class, () -> calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE,
				List.of(new StandpipeSegment("dead", PipeRole.OTHER, 0.0, null, "BI", "SCH40", "3", 120, 1, 0, null), hose))));
	}

	private StandpipeHydraulicResult withOptions(StandpipeClass standpipeClass, int additional, int laterals, boolean levels, boolean manual,
			InletSize inletSize, Integer inlets, List<StandpipeSegment> segments) {
		return calculator.calculate(new StandpipeHydraulicInput(standpipeClass, SprinklerProtection.NONE, null, null, additional, false, false,
				laterals, levels, manual, inletSize, inlets, null, null, segments));
	}

	private List<StandpipeSegment> twoConnectionsAndRiser() {
		return List.of(hosePiping("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null),
				hosePiping("h2", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null), riser("r", 0, "4", 20, 10, null));
	}

	@Test
	void singleConnectionLaterals_areTreatedAsSeparateStandpipes() {
		var segments = twoConnectionsAndRiser();

		var none = withOptions(StandpipeClass.CLASS_I, 0, 0, false, false, null, null, segments);
		var one = withOptions(StandpipeClass.CLASS_I, 0, 1, false, false, null, null, segments);
		var both = withOptions(StandpipeClass.CLASS_I, 1, 1, false, false, null, null, segments);

		assertEquals(500.0, none.requiredFlowGpm(), 1e-9);
		assertEquals(750.0, one.requiredFlowGpm(), 1e-9);
		assertEquals(1000.0, both.requiredFlowGpm(), 1e-9);
		assertTrue(one.demandLines().stream().anyMatch(l -> l.clause().equals("10.6.1.1.4") && l.description().contains("10.6.1.1.7")));
		assertTrue(none.demandLines().stream().noneMatch(l -> l.description().contains("10.6.1.1.7")));
		// Class II adds no flow for more connections, so a lateral changes nothing there.
		var classII = withOptions(StandpipeClass.CLASS_II, 0, 2, false, false, null, null,
				List.of(hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null), riser("r", 0, "3", 10, 0, null)));
		assertEquals(100.0, classII.requiredFlowGpm(), 1e-9);
		assertThrows(CalculationException.class, () -> withOptions(StandpipeClass.CLASS_I, 0, -1, false, false, null, null, segments));
	}

	@Test
	void risersAtDifferentLevels_askForASeparateCalculationPerLevel() {
		var segments = twoConnectionsAndRiser();

		assertEquals(CheckStatus.CONFIRM, status(withOptions(StandpipeClass.CLASS_I, 1, 0, true, false, null, null, segments), "10.6.1.2.1.1"));
		assertTrue(withOptions(StandpipeClass.CLASS_I, 1, 0, false, false, null, null, segments).checks().stream()
				.noneMatch(c -> c.clause().equals("10.6.1.2.1.1")));
	}

	@Test
	void manualSystem_endsAtTheFireDepartmentConnectionAndAsksForConfirmations() {
		var segments = twoConnectionsAndRiser();

		var manual = withOptions(StandpipeClass.CLASS_I, 0, 0, false, true, null, null, segments);
		var automatic = withOptions(StandpipeClass.CLASS_I, 0, 0, false, false, null, null, segments);

		assertTrue(manual.manualSystem());
		assertEquals(CheckStatus.INFO, status(manual, "10.2.6.1.2"));
		assertEquals(CheckStatus.CONFIRM, status(manual, "10.4.4"));
		assertEquals(CheckStatus.CONFIRM, status(manual, "10.2.2"));
		assertEquals(automatic.pressureAtSupplyPsi(), manual.pressureAtSupplyPsi(), 1e-9);
		assertFalse(automatic.manualSystem());
		assertTrue(automatic.checks().stream().noneMatch(c -> c.clause().equals("10.4.4") || c.clause().equals("10.2.2")));
		assertThrows(CalculationException.class, () -> withOptions(StandpipeClass.CLASS_II, 0, 0, false, true, null, null,
				List.of(hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null), riser("r", 0, "3", 10, 0, null))));
	}

	@Test
	void largeDiameterInlets_carryMoreFlowEach() {
		var segments = twoConnectionsAndRiser();
		// 1,000 gpm: add two more standpipes so the flow at the supply is 1,000.
		var small = withOptions(StandpipeClass.CLASS_I, 2, 0, false, false, InletSize.TWO_AND_HALF_INCH, 3, segments);
		var four = withOptions(StandpipeClass.CLASS_I, 2, 0, false, false, InletSize.FOUR_INCH, 2, segments);
		var five = withOptions(StandpipeClass.CLASS_I, 2, 0, false, false, InletSize.FIVE_INCH, 1, segments);
		var defaulted = withOptions(StandpipeClass.CLASS_I, 2, 0, false, false, null, null, segments);

		assertEquals(4, small.requiredFdcInlets());
		assertEquals(CheckStatus.FAIL, status(small, "10.7.3"));
		assertEquals(2, four.requiredFdcInlets());
		assertEquals(CheckStatus.PASS, status(four, "10.7.3.1.1"));
		assertEquals(2, five.requiredFdcInlets());
		assertEquals(CheckStatus.FAIL, status(five, "10.7.3.1.2"));
		assertEquals(InletSize.TWO_AND_HALF_INCH, defaulted.fdcInletSize());
		assertEquals(InletSize.FOUR_INCH, four.fdcInletSize());
		assertTrue(four.checks().stream().anyMatch(c -> c.clause().equals("10.7.3.1.1") && c.note().contains("10.7.3.1.3")));
		assertTrue(small.checks().stream().noneMatch(c -> c.note() != null && c.note().contains("10.7.3.1.3")));
	}

	private static CheckStatus status(StandpipeHydraulicResult result, String clause) {
		return result.checks().stream().filter(c -> c.clause().equals(clause)).findFirst().orElseThrow().status();
	}

}
