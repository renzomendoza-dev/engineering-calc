package com.renzoproject.calc.core.mechanical.standpipe;

import com.renzoproject.calc.core.compliance.CheckStatus;
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
		return new StandpipeSegment(label, PipeRole.HOSE_PIPING, flow, size, false, null, "BI", "SCH40", nominal, 120, length, rise, fittings);
	}

	private static StandpipeSegment riser(String label, double flow, String nominal, double length, double rise, List<StandpipeFitting> fittings) {
		return new StandpipeSegment(label, PipeRole.STANDPIPE, flow, null, false, null, "BI", "SCH40", nominal, 120, length, rise, fittings);
	}

	private static StandpipeHydraulicInput input(StandpipeClass standpipeClass, SprinklerProtection protection, List<StandpipeSegment> segments) {
		return new StandpipeHydraulicInput(standpipeClass, protection, null, null, 0, false, false, 0, false, false, null, null, null, null, null, null, null, segments);
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
				null, durationMinutes, 50.0, null, null, null, segments));
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
		StandpipeSegment atC100 = new StandpipeSegment("b", PipeRole.STANDPIPE, 100.0, null, false, null, "BI", "SCH40", "3", 100, 10, 0,
				List.of(new StandpipeFitting("TEE_OR_CROSS", 2, null)));
		StandpipeSegment explicit = new StandpipeSegment("c", PipeRole.STANDPIPE, 100.0, null, false, null, "BI", "SCH40", "3", 100, 10, 0,
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
		var drop = new StandpipeSegment("Riser top", PipeRole.STANDPIPE, 0.0, null, false, null, "BI", "SCH40", "4", 120, 3, -20, null);
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
				null, null, null, null, null, null, segments));
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
				null, null, 250.0, null, null, null, segments));

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
				false, 0, false, false, null, null, null, null, null, null, null, segments));

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
				0, false, false, 0, false, false, null, null, null, null, null, null, null, segments));
		assertEquals(150.0, combined.flowAddedAtSupplyGpm(), 1e-9);
		assertEquals(650.0, combined.flowAtSupplyGpm(), 1e-9);
	}

	@Test
	void flowAddedAtSupply_aGivenFigureIsUsedAsItIsAndClassIINeverAssumesOne() {
		var segments = List.of(hosePiping("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null),
				hosePiping("h2", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null), riser("r", 0, "4", 20, 10, null));
		var low = calculator.calculate(new StandpipeHydraulicInput(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 1, false, false, 0, false, false, null,
				null, null, 100.0, null, null, null, segments));

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
		var rise = new StandpipeSegment("rise", PipeRole.STANDPIPE, 0.0, null, false, null, "BI", "SCH40", "4", 120, 500, 100, null);
		var second = hosePiping("two", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null);
		var out = calculator.calculate(input(StandpipeClass.CLASS_III, SprinklerProtection.NONE, List.of(first, rise, second)));
		assertEquals(CheckStatus.CONFIRM, status(out, "10.2.4.1"));
	}

	@Test
	void minimumPipeSizes_applyToClassIAndIIIOnly() {
		var hose = hosePiping("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null);
		var thin = riser("Riser", 0, "3", 20, 10, null);
		var branch = new StandpipeSegment("Branch", PipeRole.BRANCH_LINE, 0.0, null, false, null, "BI", "SCH40", "2", 120, 10, 0, null);
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
		var base = new StandpipeHydraulicInput(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 0, false, false, 0, false, false, null, 1, null, 50.0, null, null, null, segments);

		var result = calculator.calculate(base);

		assertEquals(3, result.requiredFdcInlets());
		assertEquals(CheckStatus.FAIL, status(result, "10.7.3"));
	}

	@Test
	void durationAndPressureLimits() {
		var segments = List.of(hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null), riser("r", 0, "3", 10, 10, null));
		var shortSupply = calculator.calculate(new StandpipeHydraulicInput(StandpipeClass.CLASS_II, SprinklerProtection.NONE, null, null, 0, false, false, 0, false, false, null,
				null, 20.0, null, null, null, null, segments));
		assertEquals(CheckStatus.FAIL, status(shortSupply, "5.2"));
		assertEquals(CheckStatus.PASS, status(calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, segments)), "5.2"));

		var tall = List.of(hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null), riser("r", 0, "3", 10, 900, null));
		assertEquals(CheckStatus.FAIL, status(calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, tall)), "10.2.1"));
		assertEquals(CheckStatus.OPEN, status(shortSupply, "10.2.4.2"));
	}

	@Test
	void nominalSize_isReadAsInches() {
		assertEquals(2.5, StandpipePipeData.nominalInches("2-1/2"), 1e-9);
		assertEquals(4.0, StandpipePipeData.nominalInches("4"), 1e-9);
		assertEquals(0.75, StandpipePipeData.nominalInches("3/4"), 1e-9);
		assertThrows(CalculationException.class, () -> StandpipePipeData.nominalInches("big"));
	}

	@Test
	void inputRules() {
		var hose = hosePiping("h", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 5, 0, null);
		var plain = riser("r", 0, "3", 10, 0, null);

		assertThrows(CalculationException.class, () -> input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, List.of()));
		assertThrows(CalculationException.class, () -> input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, List.of(plain)));
		assertThrows(CalculationException.class, () -> input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, List.of(hose, hose)));
		assertThrows(CalculationException.class, () -> new StandpipeSegment("x", PipeRole.OTHER, null, HoseConnectionSize.ONE_AND_HALF_INCH, false, null, "BI",
				"SCH40", "3", 120, 1, 0, null));
		assertThrows(CalculationException.class, () -> new StandpipeSegment("x", PipeRole.OTHER, null, null, false, null, "BI", "SCH40", "3", 0, 1, 0, null));
		assertThrows(CalculationException.class, () -> new StandpipeFitting(null, 1, null));
		assertNull(new StandpipeSegment("x", PipeRole.OTHER, null, null, false, null, "BI", "SCH40", "3", 120, 1, 0, null).hoseConnectionSize());
		// A segment with no flow anywhere upstream carries nothing.
		assertThrows(CalculationException.class, () -> calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE,
				List.of(new StandpipeSegment("dead", PipeRole.OTHER, 0.0, null, false, null, "BI", "SCH40", "3", 120, 1, 0, null), hose))));
	}

	private StandpipeHydraulicResult withOptions(StandpipeClass standpipeClass, int additional, int laterals, boolean levels, boolean manual,
			InletSize inletSize, Integer inlets, List<StandpipeSegment> segments) {
		return calculator.calculate(new StandpipeHydraulicInput(standpipeClass, SprinklerProtection.NONE, null, null, additional, false, false,
				laterals, levels, manual, inletSize, inlets, null, null, null, null, null, segments));
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

	private StandpipeHydraulicResult withFdc(StandpipeClass standpipeClass, boolean manual, List<StandpipeFdcLine> lines) {
		return calculator.calculate(new StandpipeHydraulicInput(standpipeClass, SprinklerProtection.NONE, null, null, 0, false, false, 0, false,
				manual, null, null, null, null, null, null, lines, twoConnectionsAndRiser()));
	}

	private static StandpipeSegment fdcPipe(String label, String nominal, double length, double rise) {
		return new StandpipeSegment(label, PipeRole.OTHER, null, null, false, null, "BI", "SCH40", nominal, 120, length, rise, null);
	}

	private static CheckStatus statusOf(StandpipeHydraulicResult result, String clause, String descriptionPart) {
		return result.checks().stream().filter(c -> c.clause().equals(clause) && c.description().contains(descriptionPart)).findFirst().orElseThrow().status();
	}

	@Test
	void fdcLine_carriesTheWholeFlowAtTheSupply_andAddsItsFrictionAndElevationToTheSupplyPressure() {
		var line = new StandpipeFdcLine("Street", null, null, List.of(fdcPipe("Feed", "4", 50, 3)));

		var result = withFdc(StandpipeClass.CLASS_I, false, List.of(line));
		var row = result.fdcLines().get(0);
		var pipe = row.segments().get(0);

		assertEquals(500.0, row.flowGpm(), 1e-9);
		assertEquals(500.0, pipe.flowGpm(), 1e-9);
		double friction = 4.52 * Math.pow(500, 1.85) / (Math.pow(120, 1.85) * Math.pow(pipe.insideDiameterInch(), 4.87)) * 50;
		assertEquals(friction, pipe.frictionLossPsi(), 1e-9);
		assertEquals(result.pressureAtSupplyPsi(), row.pressureAtSystemPsi(), 1e-9);
		assertEquals(result.pressureAtSupplyPsi(), pipe.pressureAtRemoteEndPsi(), 1e-9);
		assertEquals(result.pressureAtSupplyPsi() + friction + 0.433 * 3, row.pressureAtFdcPsi(), 1e-9);
		assertEquals(row.pressureAtFdcPsi(), result.highestFdcPressurePsi(), 1e-9);
		assertEquals(CheckStatus.INFO, statusOf(result, "10.4.1", "Street"));
		assertEquals(CheckStatus.PASS, statusOf(result, "10.2.1", "Street"));
		assertEquals(CheckStatus.CONFIRM, statusOf(result, "10.2.2", "Street"));
		assertTrue(result.checks().stream().noneMatch(c -> c.clause().equals("10.4.1") && c.status() == CheckStatus.OPEN));
	}

	@Test
	void fdcLines_inSeries_addPipeByPipe_andTheHighestPressureIsReported() {
		var street = new StandpipeFdcLine("Street", null, null, List.of(fdcPipe("A", "4", 30, 0), fdcPipe("B", "4", 40, 0)));
		var yard = new StandpipeFdcLine("Yard", null, null, List.of(fdcPipe("C", "6", 20, 0)));

		var result = withFdc(StandpipeClass.CLASS_I, false, List.of(street, yard));

		var rows = result.fdcLines().get(0).segments();
		assertEquals(rows.get(0).pressureAtSupplyEndPsi(), rows.get(1).pressureAtRemoteEndPsi(), 1e-9);
		assertEquals(rows.get(1).pressureAtSupplyEndPsi(), result.fdcLines().get(0).pressureAtFdcPsi(), 1e-9);
		// The 4 in. run is the lossier one, so it sets the highest pressure.
		assertTrue(result.fdcLines().get(0).pressureAtFdcPsi() > result.fdcLines().get(1).pressureAtFdcPsi());
		assertEquals(result.fdcLines().get(0).pressureAtFdcPsi(), result.highestFdcPressurePsi(), 1e-9);
	}

	@Test
	void connectionsSharingTheDemand_mustTogetherCarryIt() {
		var half = new StandpipeFdcLine("East", 250.0, null, List.of(fdcPipe("A", "4", 30, 0)));
		var other = new StandpipeFdcLine("West", 250.0, null, List.of(fdcPipe("B", "4", 30, 0)));
		var short1 = new StandpipeFdcLine("East", 200.0, null, List.of(fdcPipe("A", "4", 30, 0)));
		var short2 = new StandpipeFdcLine("West", 200.0, null, List.of(fdcPipe("B", "4", 30, 0)));

		var enough = withFdc(StandpipeClass.CLASS_I, false, List.of(half, other));
		var notEnough = withFdc(StandpipeClass.CLASS_I, false, List.of(short1, short2));

		assertEquals(250.0, enough.fdcLines().get(0).flowGpm(), 1e-9);
		assertEquals(CheckStatus.PASS, status(enough, "10.7.3.2"));
		assertEquals(CheckStatus.FAIL, status(notEnough, "10.7.3.2"));
		// With each carrying the whole flow there is nothing to add up.
		var whole = withFdc(StandpipeClass.CLASS_I, false, List.of(new StandpipeFdcLine("Street", null, null, List.of(fdcPipe("A", "4", 30, 0)))));
		assertTrue(whole.checks().stream().noneMatch(c -> c.clause().equals("10.7.3.2")));
	}

	@Test
	void inletsOnAConnection_areCheckedAgainstTheFlowItCarries() {
		var tooFew = new StandpipeFdcLine("Street", null, 1, List.of(fdcPipe("A", "4", 30, 0)));
		var enough = new StandpipeFdcLine("Street", null, 2, List.of(fdcPipe("A", "4", 30, 0)));

		assertEquals(CheckStatus.FAIL, statusOf(withFdc(StandpipeClass.CLASS_I, false, List.of(tooFew)), "10.7.3", "Street:"));
		assertEquals(CheckStatus.PASS, statusOf(withFdc(StandpipeClass.CLASS_I, false, List.of(enough)), "10.7.3", "Street:"));
	}

	@Test
	void withNoConnectionPipingGiven_classIAndIIISayOpen_andClassIIIsNotAsked() {
		var classI = withFdc(StandpipeClass.CLASS_I, false, null);
		assertEquals(CheckStatus.OPEN, status(classI, "10.4.1"));
		assertTrue(classI.fdcLines().isEmpty());
		assertNull(classI.highestFdcPressurePsi());

		var classII = carmona(60.0);
		assertTrue(classII.checks().stream().noneMatch(c -> c.clause().equals("10.4.1")));
	}

	@Test
	void aConnectionAbove400Psi_fails() {
		var level = new StandpipeFdcLine("Roof", null, null, List.of(fdcPipe("Flat", "4", 10, 0)));
		var high = new StandpipeFdcLine("High", null, null, List.of(fdcPipe("Up", "4", 10, 900)));

		assertEquals(CheckStatus.PASS, statusOf(withFdc(StandpipeClass.CLASS_I, false, List.of(level)), "10.2.1", "Roof"));
		assertEquals(CheckStatus.FAIL, statusOf(withFdc(StandpipeClass.CLASS_I, false, List.of(high)), "10.2.1", "High"));
	}

	@Test
	void manualSystem_usesTheConnectionPressures_andChecksTheComponentsPerConnection() {
		var a = new StandpipeFdcLine("Street", null, null, List.of(fdcPipe("A", "4", 30, 0)));
		var b = new StandpipeFdcLine("Yard", null, null, List.of(fdcPipe("B", "4", 300, 0)));

		var result = withFdc(StandpipeClass.CLASS_I, true, List.of(a, b));

		var manual = result.checks().stream().filter(c -> c.clause().equals("10.2.6.1.2")).findFirst().orElseThrow();
		assertEquals(result.highestFdcPressurePsi(), manual.actual(), 1e-9);
		assertEquals(result.fdcLines().get(1).pressureAtFdcPsi(), result.highestFdcPressurePsi(), 1e-9);
		// One components check per connection, and not an extra generic one.
		assertEquals(2, result.checks().stream().filter(c -> c.clause().equals("10.2.2")).count());
		assertEquals(CheckStatus.CONFIRM, status(result, "10.4.4"));
	}

	private StandpipeHydraulicResult withFdcSized(InletSize inletSize, boolean manual, List<StandpipeFdcLine> lines) {
		return calculator.calculate(new StandpipeHydraulicInput(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 0, false, false, 0, false,
				manual, inletSize, null, null, null, null, null, lines, twoConnectionsAndRiser()));
	}

	@Test
	void pumperEnvelope_isTheConservativeBasisOfAnnexA10_7_andAPressureAboveItIsConfirmedNotFailed() {
		var flat = new StandpipeFdcLine("Street", null, null, List.of(fdcPipe("Flat", "4", 10, 0)));
		var base = withFdc(StandpipeClass.CLASS_I, false, List.of(flat));
		var check = base.checks().stream().filter(c -> c.clause().equals("A.10.7") && c.description().contains("Street")).findFirst().orElseThrow();

		// 500 gpm is the 250 psi rating point of a 1,000 gpm pumper.
		assertEquals(250.0, check.limit(), 1e-9);
		assertEquals(base.fdcLines().get(0).pressureAtFdcPsi(), check.actual(), 1e-9);
		assertEquals(CheckStatus.PASS, check.status());

		double riseFt = (400.0 - base.pressureAtSupplyPsi()) / 0.433;
		var high = new StandpipeFdcLine("Street", null, null, List.of(fdcPipe("Up", "4", 10, riseFt)));
		assertEquals(CheckStatus.CONFIRM, statusOf(withFdc(StandpipeClass.CLASS_I, false, List.of(high)), "A.10.7", "Street"));
	}

	@Test
	void pumperEnvelope_stepsDownWithTheFlow_andAskForMoreThanOnePumperAboveTheRatedFlow() {
		var at700 = withFdc(StandpipeClass.CLASS_I, false, List.of(new StandpipeFdcLine("A", 700.0, null, List.of(fdcPipe("p", "6", 10, 0)))));
		var at1000 = withFdc(StandpipeClass.CLASS_I, false, List.of(new StandpipeFdcLine("A", 1000.0, null, List.of(fdcPipe("p", "6", 10, 0)))));
		var at1200 = withFdc(StandpipeClass.CLASS_I, false, List.of(new StandpipeFdcLine("A", 1200.0, null, List.of(fdcPipe("p", "6", 10, 0)))));

		assertEquals(200.0, at700.checks().stream().filter(c -> c.clause().equals("A.10.7")).findFirst().orElseThrow().limit(), 1e-9);
		assertEquals(150.0, at1000.checks().stream().filter(c -> c.clause().equals("A.10.7")).findFirst().orElseThrow().limit(), 1e-9);
		var above = at1200.checks().stream().filter(c -> c.clause().equals("A.10.7")).findFirst().orElseThrow();
		assertEquals(CheckStatus.CONFIRM, above.status());
		assertEquals(1000.0, above.limit(), 1e-9);
		assertEquals("gpm", above.unit());
	}

	@Test
	void largeInlets_areCheckedAgainstThe185PsiWorkingPressureOfLargeSupplyHose() {
		var flat = new StandpipeFdcLine("Street", null, null, List.of(fdcPipe("Flat", "4", 10, 0)));
		double base = withFdcSized(InletSize.FOUR_INCH, false, List.of(flat)).pressureAtSupplyPsi();
		var low = withFdcSized(InletSize.FOUR_INCH, false, List.of(flat));
		var between = new StandpipeFdcLine("Street", null, null, List.of(fdcPipe("Up", "4", 10, (200.0 - base) / 0.433)));

		assertEquals(CheckStatus.PASS, statusOf(low, "A.10.7.3.1.3", "Street"));
		var over = withFdcSized(InletSize.FIVE_INCH, false, List.of(between));
		assertEquals(CheckStatus.CONFIRM, statusOf(over, "A.10.7.3.1.3", "Street"));
		assertEquals(185.0, over.checks().stream().filter(c -> c.clause().equals("A.10.7.3.1.3")).findFirst().orElseThrow().limit(), 1e-9);
		// 200 psi is above the 185 psi hose rating but within what a pumper delivers at 500 gpm.
		assertEquals(CheckStatus.PASS, statusOf(over, "A.10.7", "Street"));
		// 2.5 in. inlets are not on large supply hose.
		assertTrue(withFdcSized(InletSize.TWO_AND_HALF_INCH, false, List.of(between)).checks().stream()
				.noneMatch(c -> c.clause().equals("A.10.7.3.1.3")));
	}

	@Test
	void aManualSystemWithNoConnectionPiping_checksThePressureAtTheSupplyAgainstThePumper() {
		var manual = withFdcSized(InletSize.FOUR_INCH, true, null);
		var automatic = withFdcSized(InletSize.FOUR_INCH, false, null);

		var check = manual.checks().stream().filter(c -> c.clause().equals("A.10.7")).findFirst().orElseThrow();
		assertEquals(manual.pressureAtSupplyPsi(), check.actual(), 1e-9);
		assertTrue(check.description().startsWith("Fire department connection"));
		assertTrue(manual.checks().stream().anyMatch(c -> c.clause().equals("A.10.7.3.1.3")));
		// Without a connection to check, an automatic system has no pressure to put to the pumper.
		assertTrue(automatic.checks().stream().noneMatch(c -> c.clause().startsWith("A.10.7")));
	}

	private static StandpipeSegment hoseWithDesign(String label, double flow, HoseConnectionSize size, String nominal, Double design) {
		return new StandpipeSegment(label, PipeRole.HOSE_PIPING, flow, size, false, design, "BI", "SCH40", nominal, 120, 5, 0, null);
	}

	private List<StandpipeSegment> twoConnectionsWithTheFirstDesigned(Double design) {
		return List.of(hoseWithDesign("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", design),
				hosePiping("h2", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null), riser("r", 0, "4", 20, 10, null));
	}

	@Test
	void designResidual_holdsTheOutletAtTheHigherPressure_andRaisesThePressureAtTheSupplyByTheDifference() {
		var base = withOptions(StandpipeClass.CLASS_I, 0, 0, false, false, null, null, twoConnectionsWithTheFirstDesigned(null));
		var designed = withOptions(StandpipeClass.CLASS_I, 0, 0, false, false, null, null, twoConnectionsWithTheFirstDesigned(135.0));

		assertEquals(100.0, base.segments().get(0).pressureAtRemoteEndPsi(), 1e-9);
		assertEquals(135.0, designed.segments().get(0).pressureAtRemoteEndPsi(), 1e-9);
		assertEquals(base.pressureAtSupplyPsi() + 35.0, designed.pressureAtSupplyPsi(), 1e-9);
		var note = designed.checks().stream().filter(c -> c.clause().equals("A.10.2.6")).findFirst().orElseThrow();
		assertEquals(CheckStatus.INFO, note.status());
		assertEquals(135.0, note.limit(), 1e-9);
		assertTrue(note.note().contains("flow test"));
		assertTrue(base.checks().stream().noneMatch(c -> c.clause().equals("A.10.2.6")));
		assertEquals(CheckStatus.PASS, status(designed, "10.2.4.2"));
	}

	@Test
	void aDesignResidualBelowTheMinimumForTheSize_changesNothing_andSaysSo() {
		var base = withOptions(StandpipeClass.CLASS_I, 0, 0, false, false, null, null, twoConnectionsWithTheFirstDesigned(null));
		var low = withOptions(StandpipeClass.CLASS_I, 0, 0, false, false, null, null, twoConnectionsWithTheFirstDesigned(94.0));

		assertEquals(base.pressureAtSupplyPsi(), low.pressureAtSupplyPsi(), 1e-9);
		var note = low.checks().stream().filter(c -> c.clause().equals("A.10.2.6")).findFirst().orElseThrow();
		assertTrue(note.note().contains("minimum applies"));
	}

	@Test
	void aDesignResidualAboveWhatARegulatingDeviceAllows_fails_at175PsiOn2AndAHalfInchAnd100OnOneAndAHalf() {
		var over = withOptions(StandpipeClass.CLASS_I, 0, 0, false, false, null, null, twoConnectionsWithTheFirstDesigned(186.0));
		var atLimit = withOptions(StandpipeClass.CLASS_I, 0, 0, false, false, null, null, twoConnectionsWithTheFirstDesigned(175.0));
		assertEquals(CheckStatus.FAIL, status(over, "10.2.4.2"));
		assertEquals(CheckStatus.PASS, status(atLimit, "10.2.4.2"));

		List<StandpipeSegment> small = List.of(hoseWithDesign("A", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 120.0),
				riser("Riser", 0, "3", 400, 5, null));
		List<StandpipeSegment> smallAtLimit = List.of(hoseWithDesign("A", 100, HoseConnectionSize.ONE_AND_HALF_INCH, "1-1/2", 100.0),
				riser("Riser", 0, "3", 400, 5, null));
		assertEquals(CheckStatus.FAIL, statusOf(calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, small)), "10.2.4.1", "not above 100"));
		assertEquals(CheckStatus.PASS, statusOf(calculator.calculate(input(StandpipeClass.CLASS_II, SprinklerProtection.NONE, smallAtLimit)), "10.2.4.1", "not above 100"));
	}

	@Test
	void aDesignResidual_isForAHoseConnection_andMustBePositive() {
		assertThrows(CalculationException.class, () -> new StandpipeSegment("r", PipeRole.STANDPIPE, 0.0, null, false, 120.0, "BI", "SCH40", "4", 120, 20, 10, null));
		assertThrows(CalculationException.class, () -> hoseWithDesign("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 0.0));
		assertThrows(CalculationException.class, () -> hoseWithDesign("h", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", -5.0));
	}

	@Test
	void tableA1026_isServedAsPrintedWithItsAsterisksAndTheRowThatNamesNoNozzle() {
		var rows = new JsonStandpipeReferenceResolver().allHoseNozzleCombinations();

		assertEquals(7, rows.size());
		assertEquals(List.of(75.0, 94.0, 108.0, 118.0, 135.0, 161.0, 186.0), rows.stream().map(StandpipeHoseNozzleRow::outletPressurePsi).toList());
		assertEquals(List.of(250.0, 240.0, 210.0, 165.0, 185.0, 150.0, 150.0), rows.stream().map(StandpipeHoseNozzleRow::flowGpm).toList());
		assertEquals(List.of(false, false, true, true, true, true, true), rows.stream().map(StandpipeHoseNozzleRow::aboveMinimumResidual).toList());
		assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), rows.stream().map(StandpipeHoseNozzleRow::number).toList());
		assertNull(rows.get(6).nozzle());
		assertEquals("verify", rows.get(6).confidence());
		assertTrue(rows.subList(0, 6).stream().allMatch(row -> row.confidence().equals("verified") && row.nozzle() != null));
		assertTrue(new JsonStandpipeReferenceResolver().hoseNozzleNote().contains("flow test"));
	}

	@Test
	void standardWeightFittings_areAcceptableOnlyBelow175PsiAtTheHighestPressureInTheSystem() {
		var low = withStatic(120.0, 0.0);
		var high = withStatic(250.0, 0.0);
		var lowCheck = low.checks().stream().filter(c -> c.clause().equals("A.7.1")).findFirst().orElseThrow();
		var highCheck = high.checks().stream().filter(c -> c.clause().equals("A.7.1")).findFirst().orElseThrow();

		// The pressure required at the supply is about 130 psi here, so the static pressure only sets the highest when it is above that.
		assertEquals(175.0, lowCheck.limit(), 1e-9);
		assertEquals(Math.max(low.pressureAtSupplyPsi(), 120.0), lowCheck.actual(), 1e-9);
		assertEquals(250.0, highCheck.actual(), 1e-9);
		assertEquals(CheckStatus.CONFIRM, highCheck.status());
		assertTrue(highCheck.note().contains("static pressure at the supply"));
		assertEquals(lowCheck.actual() < 175.0 ? CheckStatus.PASS : CheckStatus.CONFIRM, lowCheck.status());
	}

	@Test
	void theHighestPressure_includesAFireDepartmentConnectionAndTheSupplyPressure() {
		var flat = new StandpipeFdcLine("Street", null, null, List.of(fdcPipe("Flat", "4", 10, 0)));
		var base = withFdc(StandpipeClass.CLASS_I, false, List.of(flat));
		var up = new StandpipeFdcLine("Street", null, null, List.of(fdcPipe("Up", "4", 10, (300.0 - base.pressureAtSupplyPsi()) / 0.433)));

		var high = withFdc(StandpipeClass.CLASS_I, false, List.of(up));
		var check = high.checks().stream().filter(c -> c.clause().equals("A.7.1")).findFirst().orElseThrow();

		assertEquals(high.highestFdcPressurePsi(), check.actual(), 1e-9);
		assertEquals(CheckStatus.CONFIRM, check.status());
		assertTrue(check.note().contains("fire department connection"));
		// With no connection and no static pressure, it is the pressure required at the supply.
		var plain = withFdc(StandpipeClass.CLASS_I, false, null);
		assertEquals(plain.pressureAtSupplyPsi(),
				plain.checks().stream().filter(c -> c.clause().equals("A.7.1")).findFirst().orElseThrow().actual(), 1e-9);
	}

	@Test
	void aManualClassIIIStandpipe_hasTheAttachedSupplyConfirmedFor100GpmAt65Psi() {
		var manual3 = withFdc(StandpipeClass.CLASS_III, true, null);
		var manual1 = withFdc(StandpipeClass.CLASS_I, true, null);
		var automatic3 = withFdc(StandpipeClass.CLASS_III, false, null);

		assertEquals(CheckStatus.CONFIRM, status(manual3, "A.8.2.3.3.1"));
		assertTrue(manual1.checks().stream().noneMatch(c -> c.clause().equals("A.8.2.3.3.1")));
		assertTrue(automatic3.checks().stream().noneMatch(c -> c.clause().equals("A.8.2.3.3.1")));
	}

	private StandpipeHydraulicResult withStatic(Double staticPsi, Double heightFt) {
		return calculator.calculate(new StandpipeHydraulicInput(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, 0, false, false, 0, false,
				false, null, null, null, null, staticPsi, heightFt, null, twoConnectionsAndRiser()));
	}

	@Test
	void staticPressure_notGiven_leavesTheCheckOpen() {
		var result = withStatic(null, null);

		assertEquals(CheckStatus.OPEN, status(result, "10.2.4.2"));
		assertNull(result.staticPressureAtLowestConnectionPsi());
		assertTrue(result.checks().stream().noneMatch(c -> c.description().contains("Static pressure at the supply")));
	}

	@Test
	void staticPressure_atOrBelow175_passes() {
		var result = withStatic(150.0, null);

		assertEquals(150.0, result.staticPressureAtLowestConnectionPsi(), 1e-9);
		assertEquals(CheckStatus.PASS, status(result, "10.2.4.2"));
		assertEquals(CheckStatus.PASS, statusOf(result, "10.2.1", "Static pressure at the supply"));
		assertEquals(CheckStatus.PASS, status(withStatic(175.0, 0.0), "10.2.4.2"));
	}

	@Test
	void staticPressure_above175_needsAPressureRegulatingDevice_confirmed_notFailed() {
		var result = withStatic(200.0, null);

		assertEquals(CheckStatus.CONFIRM, status(result, "10.2.4.2"));
		var check = result.checks().stream().filter(c -> c.clause().equals("10.2.4.2")).findFirst().orElseThrow();
		assertEquals(200.0, check.actual(), 1e-9);
		assertEquals(175.0, check.limit(), 1e-9);
		// Both connection sizes and the Class I annex note are named so the reader can decide.
		assertTrue(check.note().contains("10.2.4.1.3") && check.note().contains("A.10.2.4"));
	}

	@Test
	void staticPressure_atTheLowestConnection_isTheSupplyStaticLessTheHeadOfItsHeight() {
		// 100 ft above the supply costs 43.3 psi; 50 ft below adds 21.65 psi.
		var above = withStatic(200.0, 100.0);
		var below = withStatic(160.0, -50.0);

		assertEquals(200.0 - 43.3, above.staticPressureAtLowestConnectionPsi(), 1e-9);
		assertEquals(CheckStatus.PASS, status(above, "10.2.4.2"));
		assertEquals(160.0 + 21.65, below.staticPressureAtLowestConnectionPsi(), 1e-9);
		assertEquals(CheckStatus.CONFIRM, status(below, "10.2.4.2"));
	}

	@Test
	void staticPressure_atTheSupplyAbove400_fails() {
		assertEquals(CheckStatus.FAIL, statusOf(withStatic(450.0, 0.0), "10.2.1", "Static pressure at the supply"));
		assertEquals(CheckStatus.PASS, statusOf(withStatic(400.0, 0.0), "10.2.1", "Static pressure at the supply"));
	}

	@Test
	void staticPressureInputRules() {
		assertThrows(CalculationException.class, () -> withStatic(null, 10.0));
		assertThrows(CalculationException.class, () -> withStatic(0.0, null));
		assertThrows(CalculationException.class, () -> withStatic(-5.0, null));
	}

	private static StandpipeSegment otherStandpipeNode(String label, double flow, double length, double rise) {
		return new StandpipeSegment(label, PipeRole.OTHER, flow, null, true, null, "BI", "SCH40", "4", 120, length, rise, null);
	}

	private static StandpipeSegment plainPipe(String label, double length, double rise) {
		return new StandpipeSegment(label, PipeRole.OTHER, null, null, false, null, "BI", "SCH40", "4", 120, length, rise, null);
	}

	private StandpipeHydraulicResult withOthers(int additional, List<StandpipeSegment> segments) {
		return calculator.calculate(new StandpipeHydraulicInput(StandpipeClass.CLASS_I, SprinklerProtection.NONE, null, null, additional, false, false, 0,
				false, false, null, null, null, null, null, null, null, segments));
	}

	private static SegmentRow rowOf(StandpipeHydraulicResult result, String label) {
		return result.segments().stream().filter(r -> r.label().equals(label)).findFirst().orElseThrow();
	}

	@Test
	void anotherStandpipesConnectionPoint_isHeldAtTheMinimumResidual_whenThePathGivesLess() {
		// The path falls 100 ft after the two hose connections, so the pressure at the next node is below 100 psi.
		var segments = List.of(hosePiping("h1", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null),
				hosePiping("h2", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null), plainPipe("Down", 10, -100),
				otherStandpipeNode("To B", 250, 20, 0), riser("r", 0, "4", 20, 0, null));

		var result = withOthers(1, segments);
		var node = rowOf(result, "To B");

		assertTrue(rowOf(result, "Down").pressureAtSupplyEndPsi() < 100.0);
		assertEquals(100.0, node.pressureAtRemoteEndPsi(), 1e-9);
		assertTrue(node.residualRaised());
		assertTrue(node.otherStandpipeConnection());
		assertEquals(750.0, node.flowGpm(), 1e-9);
		assertEquals(CheckStatus.PASS, status(result, "10.6.1.2.1"));
		var check = result.checks().stream().filter(c -> c.clause().equals("10.6.1.2.1")).findFirst().orElseThrow();
		assertEquals(100.0, check.actual(), 1e-9);
		assertTrue(check.note().contains("10.6.1.2.4"));
	}

	@Test
	void anotherStandpipesConnectionPoint_isLeftAloneWhenThePathAlreadyGivesMore() {
		var segments = List.of(hosePiping("h1", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null),
				hosePiping("h2", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null), plainPipe("Up", 10, 40),
				otherStandpipeNode("To B", 250, 20, 0), riser("r", 0, "4", 20, 0, null));

		var result = withOthers(1, segments);
		var node = rowOf(result, "To B");

		assertTrue(node.pressureAtRemoteEndPsi() > 100.0);
		assertFalse(node.residualRaised());
		assertEquals(rowOf(result, "Up").pressureAtSupplyEndPsi(), node.pressureAtRemoteEndPsi(), 1e-9);
	}

	@Test
	void flowPlacedAtAnotherStandpipesConnectionPoint_countsTowardTheDemand_soNothingIsAddedAtTheSupply() {
		var segments = List.of(hosePiping("h1", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null),
				hosePiping("h2", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null), otherStandpipeNode("To B", 250, 20, 0),
				riser("r", 0, "4", 20, 0, null));

		var result = withOthers(1, segments);

		assertEquals(750.0, result.requiredFlowGpm(), 1e-9);
		assertEquals(0.0, result.flowAddedAtSupplyGpm(), 1e-9);
		assertEquals(750.0, result.flowAtSupplyGpm(), 1e-9);
		assertTrue(result.checks().stream().noneMatch(c -> c.description().contains("joining at the supply end")));
		// The hose connection check still counts only the hose connections of the most remote standpipe.
		assertEquals(CheckStatus.PASS, status(result, "10.6.1.1"));
	}

	@Test
	void otherStandpipesJoiningAtTheSupplyEnd_needAtLeastTheMinimumResidualThere() {
		var enough = withOthers(1, twoConnectionsAndRiser());
		var check = enough.checks().stream().filter(c -> c.description().contains("joining at the supply end")).findFirst().orElseThrow();

		assertEquals(CheckStatus.PASS, check.status());
		assertTrue(enough.pressureAtSupplyPsi() > 100.0);
		assertTrue(check.note().contains("more than the minimum"));
		// With nothing joining at the supply end there is no such line.
		assertTrue(withOthers(0, twoConnectionsAndRiser()).checks().stream().noneMatch(c -> c.description().contains("joining at the supply end")));
	}

	@Test
	void otherStandpipesJoiningAtTheSupplyEnd_raiseThePressureRequiredThereWhenThePathGivesLess() {
		var falling = List.of(hosePiping("h1", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null),
				hosePiping("h2", 250, HoseConnectionSize.TWO_AND_HALF_INCH, "2-1/2", 5, 0, null), riser("r", 0, "4", 20, -100, null));

		var result = withOthers(1, falling);
		var check = result.checks().stream().filter(c -> c.description().contains("joining at the supply end")).findFirst().orElseThrow();

		assertEquals(100.0, result.pressureAtSupplyPsi(), 1e-9);
		assertTrue(check.note().contains("raised"));
		// Without the other standpipe the path's own, lower pressure stands.
		assertTrue(withOthers(0, falling).pressureAtSupplyPsi() < 100.0);
	}

	@Test
	void otherStandpipeConnectionRules() {
		assertThrows(CalculationException.class, () -> new StandpipeSegment("x", PipeRole.OTHER, 250.0, HoseConnectionSize.TWO_AND_HALF_INCH, true, null,
				"BI", "SCH40", "4", 120, 10, 0, null));
		assertThrows(CalculationException.class, () -> new StandpipeSegment("x", PipeRole.OTHER, null, null, true, null, "BI", "SCH40", "4", 120, 10, 0, null));
		assertThrows(CalculationException.class, () -> new StandpipeSegment("x", PipeRole.OTHER, 0.0, null, true, null, "BI", "SCH40", "4", 120, 10, 0, null));
		// A fire department connection's pipe carries no other flow, so it cannot hold another standpipe's connection point.
		assertThrows(CalculationException.class, () -> new StandpipeFdcLine("L", null, null, List.of(otherStandpipeNode("n", 250, 10, 0))));
	}

	@Test
	void fdcLineInputRules() {
		var pipe = fdcPipe("A", "4", 30, 0);
		var hose = new StandpipeSegment("H", PipeRole.HOSE_PIPING, 100.0, HoseConnectionSize.ONE_AND_HALF_INCH, false, null, "BI", "SCH40", "1-1/2", 120, 5, 0, null);
		var withFlowAdded = new StandpipeSegment("F", PipeRole.OTHER, 50.0, null, false, null, "BI", "SCH40", "4", 120, 5, 0, null);

		assertThrows(CalculationException.class, () -> new StandpipeFdcLine(" ", null, null, List.of(pipe)));
		assertThrows(CalculationException.class, () -> new StandpipeFdcLine("L", null, null, List.of()));
		assertThrows(CalculationException.class, () -> new StandpipeFdcLine("L", 0.0, null, List.of(pipe)));
		assertThrows(CalculationException.class, () -> new StandpipeFdcLine("L", null, -1, List.of(pipe)));
		assertThrows(CalculationException.class, () -> new StandpipeFdcLine("L", null, null, List.of(hose)));
		assertThrows(CalculationException.class, () -> new StandpipeFdcLine("L", null, null, List.of(withFlowAdded)));
		assertThrows(CalculationException.class, () -> new StandpipeFdcLine("L", null, null, List.of(pipe, pipe)));
		var line = new StandpipeFdcLine("L", null, null, List.of(pipe));
		// Class II has no fire department connection requirement, and a label may not repeat.
		assertThrows(CalculationException.class, () -> withFdc(StandpipeClass.CLASS_II, false, List.of(line)));
		assertThrows(CalculationException.class, () -> withFdc(StandpipeClass.CLASS_I, false, List.of(line, line)));
	}

	private static CheckStatus status(StandpipeHydraulicResult result, String clause) {
		return result.checks().stream().filter(c -> c.clause().equals(clause)).findFirst().orElseThrow().status();
	}

}
