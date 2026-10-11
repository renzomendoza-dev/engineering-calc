package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.JsonPipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityCalculator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The supplies that are not a flow test: an elevated tank, and no supply at all.
 *
 * <p>Every case uses the same one-sprinkler network, whose demand side is hand-calculated in
 * {@link SprinklerHydraulicCalculatorTest}: S1 flows 5.6*sqrt(7) = 14.8162073 gpm, and the 1"
 * branch loses 0.0747662 psi/ft * 10 ft = 0.747662 psi, so the system needs 7.747662 psi at the
 * connection. Only what the supply offers against that changes here.
 */
class SprinklerWaterSupplyVariantsTest {

	private static final double DEMAND_GPM = 14.8162073419617;
	private static final double REQUIRED_PSI = 7.7476618357046565;

	/** 2" Sch 40 (ID 2.066142 in) at the demand flow, C = 120 -- from the hand calculation. */
	private static final double TWO_INCH_PSI_PER_FOOT = 0.0027522436572209;

	private final JsonPipeDimensionResolver dimensionResolver = new JsonPipeDimensionResolver();
	private final SprinklerHydraulicCalculator calculator = new SprinklerHydraulicCalculator(
			dimensionResolver,
			new JsonFittingEquivalentLengthResolver(),
			new PipeVelocityCalculator(dimensionResolver));

	private SprinklerHydraulicResult calculate(WaterSupply supply) {
		return calculator.calculate(new SprinklerHydraulicInput(
				SprinklerSystemType.WET,
				SprinklerHazard.LIGHT,
				0.10,
				1500.0,
				0.0,
				null,
				7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(new SprinklerPipeSegment("S1", "RISER", "BI", "SCH40", "1", 120.0, 10.0, 0.0, List.of())),
				supply,
				List.of()));
	}

	@Test
	void gravityTankWithoutOutletPiping_creditsTheFullHeadAtEveryFlow() {
		// 100 ft of water above the connection = 0.433 * 100 = 43.3 psi, with nothing to lose it to.
		SprinklerHydraulicResult result = calculate(new GravityTankSupply(100.0, List.of()));

		assertEquals(DEMAND_GPM, result.totalDemandFlowGpm(), 1e-9);
		assertEquals(REQUIRED_PSI, result.requiredPressureAtSupplyPsi(), 1e-9);
		assertEquals(43.3, result.availablePressureAtDemandPsi(), 1e-9);
		assertEquals(43.3 - REQUIRED_PSI, result.safetyMarginPsi(), 1e-9);
		assertTrue(result.passes());
		assertTrue(
				result.warnings().stream().anyMatch(w -> w.contains("no outlet piping")),
				"a tank with no piping is the optimistic reading and should say so: " + result.warnings());
	}

	@Test
	void gravityTankWithOutletPiping_losesTheRunsFrictionAtTheDemandFlow() {
		// 500 ft of 2" Sch 40 at the demand flow: 0.0027522437 * 500 = 1.3761218 psi off the head.
		SprinklerHydraulicResult result = calculate(new GravityTankSupply(
				100.0,
				List.of(new SupplyPipeRun("BI", "SCH40", "2", 120.0, 500.0, List.of()))));

		double expectedAvailable = 43.3 - TWO_INCH_PSI_PER_FOOT * 500.0;
		assertEquals(expectedAvailable, result.availablePressureAtDemandPsi(), 1e-6);
		assertEquals(41.9238782, result.availablePressureAtDemandPsi(), 1e-6);
		assertEquals(expectedAvailable - REQUIRED_PSI, result.safetyMarginPsi(), 1e-6);
		assertTrue(result.passes());
	}

	@Test
	void outletRunFittings_addToItsLengthThroughTheSameResolverAsASegment() {
		SprinklerHydraulicResult plain = calculate(new GravityTankSupply(
				100.0,
				List.of(new SupplyPipeRun("BI", "SCH40", "2", 120.0, 500.0, List.of()))));
		SprinklerHydraulicResult withFitting = calculate(new GravityTankSupply(
				100.0,
				List.of(new SupplyPipeRun("BI", "SCH40", "2", 120.0, 500.0,
						List.of(new SegmentFitting(null, 2, 10.0))))));

		// Two fittings at an explicit 10 ft each = 20 ft more pipe to lose pressure in.
		// 1e-6, not 1e-9: the psi/ft constant above is the hand figure rounded at the 13th digit,
		// which drifts by a few parts in 10^9 once multiplied out over the run.
		double extraLoss = TWO_INCH_PSI_PER_FOOT * 20.0;
		assertEquals(
				plain.availablePressureAtDemandPsi() - extraLoss,
				withFitting.availablePressureAtDemandPsi(),
				1e-6);
	}

	@Test
	void gravityTankCurve_isSampledAcrossTwiceTheDemandAndIncludesIt() {
		SprinklerHydraulicResult result = calculate(new GravityTankSupply(100.0, List.of()));

		List<SupplyCurvePoint> curve = result.supplyCurve();
		assertTrue(curve.size() >= 5, "expected the quarter points plus the demand: " + curve);
		assertEquals(0.0, curve.get(0).flowGpm(), 1e-9);
		assertEquals(DEMAND_GPM * 2, curve.get(curve.size() - 1).flowGpm(), 1e-9);
		assertTrue(
				curve.stream().anyMatch(point -> Math.abs(point.flowGpm() - DEMAND_GPM) < 1e-9),
				"the demand point itself should be on the plotted curve: " + curve);
	}

	@Test
	void noSupply_answersTheDemandAndLeavesTheVerdictUnanswered() {
		SprinklerHydraulicResult result = calculate(null);

		// The demand half is identical to the same network with a supply -- it never depended on one.
		assertEquals(DEMAND_GPM, result.totalDemandFlowGpm(), 1e-9);
		assertEquals(REQUIRED_PSI, result.requiredPressureAtSupplyPsi(), 1e-9);
		assertEquals(1, result.segments().size());

		assertNull(result.availablePressureAtDemandPsi());
		assertNull(result.safetyMarginPsi());
		assertNull(result.safetyMarginPercent());
		// Null, not false: nothing failed, nothing was asked.
		assertNull(result.passes());
		assertTrue(result.supplyCurve().isEmpty());
		assertFalseHasSupply(result);
		assertTrue(
				result.warnings().stream().anyMatch(w -> w.contains("No water supply given")),
				"the missing comparison should be stated, not left silent: " + result.warnings());
	}

	private static void assertFalseHasSupply(SprinklerHydraulicResult result) {
		assertEquals(false, result.hasSupply());
	}

	@Test
	void supplyAndNoSupply_agreeOnEveryDemandSideFigure() {
		SprinklerHydraulicResult withSupply = calculate(new SupplyCurve(80.0, 60.0, 1000.0));
		SprinklerHydraulicResult without = calculate(null);

		assertEquals(withSupply.totalDemandFlowGpm(), without.totalDemandFlowGpm(), 0.0);
		assertEquals(withSupply.requiredPressureAtSupplyPsi(), without.requiredPressureAtSupplyPsi(), 0.0);
		assertEquals(withSupply.segments(), without.segments());
	}

	@Test
	void tankAtOrBelowTheConnection_isRejectedAsASupply() {
		// Not a gravity supply at all: it is a pump's source, and the message says so rather than
		// quietly producing a zero or negative head.
		for (double height : new double[] {0.0, -12.0}) {
			CalculationException thrown =
					assertThrows(CalculationException.class, () -> new GravityTankSupply(height, List.of()));
			assertTrue(
					thrown.getMessage().contains("waterLevelHeightFt must be positive"),
					"unexpected message: " + thrown.getMessage());
			assertTrue(
					thrown.getMessage().contains("no water supply"),
					"the message should point at the way out: " + thrown.getMessage());
		}
	}

	@Test
	void tankRunOutOfHead_deliversNothingRatherThanANegativePush() {
		// A tiny head behind a very long run: friction exceeds it well before the demand flow.
		SprinklerHydraulicResult result = calculate(new GravityTankSupply(
				1.0,
				List.of(new SupplyPipeRun("BI", "SCH40", "1", 120.0, 5000.0, List.of()))));

		assertEquals(0.0, result.availablePressureAtDemandPsi(), 0.0);
		assertTrue(result.safetyMarginPsi() < 0);
		assertEquals(false, result.passes());
	}

}
