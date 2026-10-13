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
 * Uses the real pipe dimension data (BI Schedule 40) rather than a fake, because the hand
 * calculation below is anchored to published inside diameters -- 1" = 26.64 mm = 1.0488 in and
 * 2" = 52.48 mm = 2.0661 in. Fitting equivalent lengths use the real (deliberately unpopulated)
 * reference file, so the placeholder behaviour is exercised as shipped.
 */
class SprinklerHydraulicCalculatorTest {

	private final JsonPipeDimensionResolver dimensionResolver = new JsonPipeDimensionResolver();
	private final SprinklerHydraulicCalculator calculator = new SprinklerHydraulicCalculator(
			dimensionResolver,
			new JsonFittingEquivalentLengthResolver(),
			new PipeVelocityCalculator(dimensionResolver));

	private static final SupplyCurve SUPPLY = new SupplyCurve(80.0, 60.0, 1000.0);

	private static SprinklerPipeSegment segment(
			String from, String to, String nominalSize, double lengthFt, double elevationFt) {
		return new SprinklerPipeSegment(from, to, "BI", "SCH40", nominalSize, 120.0, lengthFt, elevationFt, List.of());
	}

	@Test
	void oneSprinklerOneBranchOneRiser_matchesHandCalculation() {
		// HAND CALCULATION (NFPA 13, Hazen-Williams, psi/gpm/ft/in)
		//
		// Sprinkler S1: K=5.6, coverage 130 ft2, density 0.10 gpm/ft2
		//   minimum flow      = 0.10 * 130 = 13.0 gpm
		//   pressure for that = (13.0/5.6)^2 = 5.389 psi
		//   assumed start     = 7.0 psi  -> 7.0 governs (it is the larger)
		//   flow              = 5.6 * sqrt(7) = 14.8162 gpm
		//
		// Segment S1->B1: 1" Sch 40 (ID 1.048819 in), C=120, 10 ft, level
		//   p = 4.52 * 14.8162^1.85 / (120^1.85 * 1.048819^4.87) = 0.0747662 psi/ft
		//   friction = 0.0747662 * 10 = 0.747662 psi
		//   pressure at B1 = 7.0 + 0.747662 = 7.747662 psi
		//
		// Segment B1->RISER: 2" Sch 40 (ID 2.066142 in), C=120, 12 ft, +10 ft rise
		//   p = 4.52 * 14.8162^1.85 / (120^1.85 * 2.066142^4.87) = 0.00275224 psi/ft
		//   friction  = 0.00275224 * 12 = 0.0330269 psi
		//   elevation = 0.433 * 10      = 4.33 psi
		//   pressure at RISER = 7.747662 + 0.0330269 + 4.33 = 12.110689 psi
		//
		// Alarm valve device loss          = 5.0 psi
		//   required at supply = 12.110689 + 5.0 = 17.110689 psi
		//
		// Hose stream 100 gpm added AT THE RISER (not at the sprinkler), so it does not flow
		// through either pipe above:
		//   demand = 14.8162 + 100 = 114.8162 gpm
		//
		// Supply curve 80 psi static / 60 psi residual at 1000 gpm:
		//   available = 80 - 20 * (114.8162/1000)^1.85 = 79.635216 psi
		//   margin    = 79.635216 - 17.110689 = 62.524527 psi = 78.51% -> passes
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET,
				SprinklerHazard.LIGHT,
				0.10,
				1500.0,
				100.0,
				"RISER",
				7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(segment("S1", "B1", "1", 10.0, 0.0), segment("B1", "RISER", "2", 12.0, 10.0)),
				SUPPLY,
				List.of(new DeviceLoss("Alarm valve", 5.0)));

		SprinklerHydraulicResult result = calculator.calculate(input);

		assertEquals(2, result.segments().size());

		SegmentFlowRow branch = result.segments().get(0);
		assertEquals("S1", branch.fromNodeId());
		assertEquals("B1", branch.toNodeId());
		assertEquals(14.8162073, branch.flowGpm(), 1e-6);
		assertEquals(1.048818898, branch.insideDiameterInch(), 1e-9);
		assertEquals(0.0747662, branch.frictionLossPsiPerFoot(), 1e-7);
		assertEquals(0.747662, branch.frictionLossPsi(), 1e-6);
		assertEquals(0.0, branch.elevationPressurePsi(), 1e-9);
		assertEquals(7.747662, branch.endPressurePsi(), 1e-6);
		assertEquals(5.502064, branch.velocityFps(), 1e-5);

		SegmentFlowRow riser = result.segments().get(1);
		assertEquals("RISER", riser.toNodeId());
		assertEquals(14.8162073, riser.flowGpm(), 1e-6, "hose stream must not flow through the riser pipe here");
		assertEquals(2.066141732, riser.insideDiameterInch(), 1e-9);
		assertEquals(0.00275224, riser.frictionLossPsiPerFoot(), 1e-8);
		assertEquals(0.0330269, riser.frictionLossPsi(), 1e-7);
		assertEquals(4.33, riser.elevationPressurePsi(), 1e-9);
		assertEquals(12.110689, riser.endPressurePsi(), 1e-6);
		assertEquals(1.417772, riser.velocityFps(), 1e-5);

		assertEquals(114.8162073, result.totalDemandFlowGpm(), 1e-6);
		assertEquals(17.110689, result.requiredPressureAtSupplyPsi(), 1e-6);
		assertEquals(79.635216, result.availablePressureAtDemandPsi(), 1e-6);
		assertEquals(62.524527, result.safetyMarginPsi(), 1e-6);
		assertEquals(78.5137, result.safetyMarginPercent(), 1e-4);
		assertTrue(result.passes());
		assertTrue(result.warnings().isEmpty(), "expected no warnings, got " + result.warnings());
	}

	@Test
	void minimumFlowGoverns_whenAssumedStartPressureIsLower() {
		// density 0.20 over 130 ft2 = 26 gpm -> (26/5.6)^2 = 21.556 psi, well above the 7 psi
		// assumed start, so the minimum flow governs instead.
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.ORDINARY_GROUP_2, 0.20, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(segment("S1", "RISER", "2", 1.0, 0.0)),
				SUPPLY, List.of());

		SprinklerHydraulicResult result = calculator.calculate(input);

		assertEquals(26.0, result.segments().get(0).flowGpm(), 1e-9);
	}

	@Test
	void junctionBalancing_scalesTheWeakerBranchAndTakesTheGoverningPressure() {
		// Two identical sprinklers, but branch B runs 20 ft of 1" where branch A runs 10 ft, so B
		// arrives at the junction at the higher pressure and governs:
		//   A arrives 7.0 + 0.747662 = 7.747662 psi
		//   B arrives 7.0 + 1.495324 = 8.495324 psi  <- governs
		//   factor for A = sqrt(8.495324 / 7.747662) = 1.047140
		//   A flow       = 14.8162 * 1.047140 = 15.514639 gpm
		//   total at J   = 15.514639 + 14.816207 = 30.330847 gpm
		//   riser 2" x 10 ft at that flow = 0.0103588 psi/ft -> 0.103588 psi
		//   pressure at RISER = 8.495324 + 0.103588 = 8.598912 psi
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("A1", 5.6, 130.0), new RemoteAreaSprinkler("B1", 5.6, 130.0)),
				List.of(
						segment("A1", "J", "1", 10.0, 0.0),
						segment("B1", "J", "1", 20.0, 0.0),
						segment("J", "RISER", "2", 10.0, 0.0)),
				SUPPLY, List.of());

		SprinklerHydraulicResult result = calculator.calculate(input);

		assertEquals(3, result.segments().size());
		SegmentFlowRow scaledBranch = row(result, "A1", "J");
		SegmentFlowRow governingBranch = row(result, "B1", "J");
		SegmentFlowRow riser = row(result, "J", "RISER");

		assertEquals(15.514639, scaledBranch.flowGpm(), 1e-6, "weaker branch should be scaled up");
		assertEquals(14.816207, governingBranch.flowGpm(), 1e-6, "governing branch keeps its flow");
		// The rescaled branch's friction is recomputed at the scaled flow, not left at the old one.
		assertEquals(0.08141676, scaledBranch.frictionLossPsiPerFoot(), 1e-8);
		assertEquals(30.330847, riser.flowGpm(), 1e-6);
		assertEquals(8.598912, riser.endPressurePsi(), 1e-6);
	}

	@Test
	void hoseStream_addedAtAnUpstreamNode_flowsThroughTheDownstreamPipe() {
		// Same network as the hand calculation, but the hose allowance is taken at B1 instead of
		// the riser, so the riser pipe must now carry sprinkler flow + hose.
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 100.0, "B1", 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(segment("S1", "B1", "1", 10.0, 0.0), segment("B1", "RISER", "2", 12.0, 0.0)),
				SUPPLY, List.of());

		SprinklerHydraulicResult result = calculator.calculate(input);

		assertEquals(14.8162073, row(result, "S1", "B1").flowGpm(), 1e-6);
		assertEquals(114.8162073, row(result, "B1", "RISER").flowGpm(), 1e-6);
	}

	@Test
	void publishedFittingLengths_areRescaledForADrySystemsLowerCFactor() {
		// NFPA 13's chart is in feet of C=120 pipe. On C=100 dry steel the two 2" 90 degree
		// elbows (5 ft each published) become 10 * (100/120)^1.85 = 10 * 0.713698 = 7.13698 ft,
		// so the segment totals 10 + 7.13698 = 17.13698 ft.
		SprinklerPipeSegment drySegment = new SprinklerPipeSegment(
				"S1", "RISER", "BI", "SCH40", "2", 100.0, 10.0, 0.0,
				List.of(new SegmentFitting("ELBOW_90_STANDARD", 2, null)));

		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.DRY, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(drySegment), SUPPLY, List.of());

		SegmentFlowRow row = calculator.calculate(input).segments().get(0);

		assertEquals(0.713698, row.fittingCFactorAdjustment(), 1e-6);
		assertEquals(7.13698, row.fittingEquivalentLengthFt(), 1e-5);
		assertEquals(17.13698, row.totalLengthFt(), 1e-5);
	}

	@Test
	void explicitFittingLengths_areNeverRescaled() {
		// The caller's basis is unknown, so an explicitly supplied length is used as given even
		// on a C=100 segment, and the reported adjustment stays 1.0.
		SprinklerPipeSegment drySegment = new SprinklerPipeSegment(
				"S1", "RISER", "BI", "SCH40", "2", 100.0, 10.0, 0.0,
				List.of(new SegmentFitting(null, 2, 5.0)));

		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.DRY, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(drySegment), SUPPLY, List.of());

		SegmentFlowRow row = calculator.calculate(input).segments().get(0);

		assertEquals(10.0, row.fittingEquivalentLengthFt(), 1e-9);
		assertEquals(1.0, row.fittingCFactorAdjustment(), 1e-9);
	}

	@Test
	void fittingWithExplicitEquivalentLength_isAddedToTheSegmentLength() {
		SprinklerPipeSegment withFittings = new SprinklerPipeSegment(
				"S1", "RISER", "BI", "SCH40", "2", 120.0, 10.0, 0.0,
				List.of(new SegmentFitting("ELBOW_90_STANDARD", 2, 5.0)));

		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(withFittings), SUPPLY, List.of());

		SegmentFlowRow row = calculator.calculate(input).segments().get(0);

		assertEquals(10.0, row.fittingEquivalentLengthFt(), 1e-9, "2 fittings x 5 ft each");
		assertEquals(20.0, row.totalLengthFt(), 1e-9);
	}

	@Test
	void unverifiedFittingRow_stillWarns() {
		// The vane type flow switch is the one row that failed its second read -- neither
		// independent reproduction of the NFPA chart contains it. Until that is settled, any
		// calculation leaning on it has to say so.
		SprinklerPipeSegment withFlowSwitch = new SprinklerPipeSegment(
				"S1", "RISER", "BI", "SCH40", "2", 120.0, 10.0, 0.0,
				List.of(new SegmentFitting("VANE_TYPE_FLOW_SWITCH", 1, null)));

		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(withFlowSwitch), SUPPLY, List.of());

		SprinklerHydraulicResult result = calculator.calculate(input);

		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("tagged 'verify'")),
				"expected a reference-data warning, got " + result.warnings());
	}

	@Test
	void fittingLookedUpFromThePublishedTable_isAddedToTheSegmentLength() {
		// NFPA 13 Table 27.2.3.1.1: a 90 degree standard elbow on 2" pipe is 5 ft, so two of them
		// add 10 ft to the segment's 10 ft of pipe.
		SprinklerPipeSegment withFittings = new SprinklerPipeSegment(
				"S1", "RISER", "BI", "SCH40", "2", 120.0, 10.0, 0.0,
				List.of(new SegmentFitting("ELBOW_90_STANDARD", 2, null)));

		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(withFittings), SUPPLY, List.of());

		SprinklerHydraulicResult result = calculator.calculate(input);
		SegmentFlowRow row = result.segments().get(0);

		assertEquals(10.0, row.fittingEquivalentLengthFt(), 1e-9);
		assertEquals(1.0, row.fittingCFactorAdjustment(), 1e-9, "C=120 is the chart's own basis");
		assertEquals(20.0, row.totalLengthFt(), 1e-9);
		// This row has passed its second read against the printed chart, so using it is silent.
		assertTrue(result.warnings().stream().noneMatch(warning -> warning.contains("tagged 'verify'")),
				"a verified fitting row should not warn, got " + result.warnings());
	}

	@Test
	void fittingSizeTheTableLeavesBlank_throwsRatherThanAssumingZero() {
		// Table 27.2.3.1.1 publishes no butterfly valve below 2", so a 1-1/2" one has no length.
		SprinklerPipeSegment withFittings = new SprinklerPipeSegment(
				"S1", "RISER", "BI", "SCH40", "1-1/2", 120.0, 10.0, 0.0,
				List.of(new SegmentFitting("BUTTERFLY_VALVE", 1, null)));

		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(withFittings), SUPPLY, List.of());

		CalculationException thrown = assertThrows(CalculationException.class, () -> calculator.calculate(input));
		assertTrue(thrown.getMessage().contains("No published equivalent length"), thrown.getMessage());
	}

	@Test
	void loopedNetwork_throws() {
		// A1 -> A2 -> A1 is a loop; C1 -> RISER keeps exactly one supply point so the loop check
		// is what fires, not the supply-point check.
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("C1", 5.6, 130.0)),
				List.of(
						segment("A1", "A2", "1", 10.0, 0.0),
						segment("A2", "A1", "1", 10.0, 0.0),
						segment("C1", "RISER", "2", 10.0, 0.0)),
				SUPPLY, List.of());

		CalculationException thrown = assertThrows(CalculationException.class, () -> calculator.calculate(input));
		assertTrue(thrown.getMessage().contains("loop"), thrown.getMessage());
	}

	@Test
	void nodeWithTwoOutgoingSegments_throws() {
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(segment("S1", "B1", "1", 10.0, 0.0), segment("S1", "B2", "1", 10.0, 0.0)),
				SUPPLY, List.of());

		CalculationException thrown = assertThrows(CalculationException.class, () -> calculator.calculate(input));
		assertTrue(thrown.getMessage().contains("more than one outgoing"), thrown.getMessage());
	}

	@Test
	void twoDisconnectedTrees_throwOnTheSupplyPointCheck() {
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(segment("S1", "R1", "1", 10.0, 0.0), segment("S2", "R2", "1", 10.0, 0.0)),
				SUPPLY, List.of());

		CalculationException thrown = assertThrows(CalculationException.class, () -> calculator.calculate(input));
		assertTrue(thrown.getMessage().contains("exactly one supply point"), thrown.getMessage());
	}

	@Test
	void sprinklerNotOnTheNetwork_throws() {
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("NOWHERE", 5.6, 130.0)),
				List.of(segment("S1", "RISER", "1", 10.0, 0.0)),
				SUPPLY, List.of());

		CalculationException thrown = assertThrows(CalculationException.class, () -> calculator.calculate(input));
		assertTrue(thrown.getMessage().contains("not an end of any"), thrown.getMessage());
	}

	@Test
	void deadEndWithoutASprinkler_throws() {
		// B1 has nothing flowing into it and no sprinkler, so there is no flow source.
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(
						segment("S1", "J", "1", 10.0, 0.0),
						segment("B1", "J", "1", 10.0, 0.0),
						segment("J", "RISER", "2", 10.0, 0.0)),
				SUPPLY, List.of());

		CalculationException thrown = assertThrows(CalculationException.class, () -> calculator.calculate(input));
		assertTrue(thrown.getMessage().contains("no sprinkler"), thrown.getMessage());
	}

	@Test
	void velocityAbove20Fps_warnsWithoutFailing() {
		// 0.40 gpm/ft2 over 100 ft2 = 40 gpm through 3/4" (ID 0.8252 in) = 24.0 fps.
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.EXTRA_GROUP_2, 0.40, 2500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 100.0)),
				List.of(segment("S1", "RISER", "3/4", 5.0, 0.0)),
				SUPPLY, List.of());

		SprinklerHydraulicResult result = calculator.calculate(input);

		assertEquals(23.9958, result.segments().get(0).velocityFps(), 1e-4);
		assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("above the 20.0 fps")),
				"expected a velocity warning, got " + result.warnings());
		// It says what the pipe is carrying and in what size, so the fix is evident from the line.
		String flow = String.valueOf(Math.round(result.segments().get(0).flowGpm() * 100.0) / 100.0);
		assertTrue(result.warnings().stream().anyMatch(
						warning -> warning.contains("(" + flow + " gpm in 3/4\" pipe)")),
				"expected the flow and size in the warning, got " + result.warnings());
		assertTrue(result.passes(), "a high velocity is a warning, not a failure");
	}

	@Test
	void demandBeyondTheSupplyCurve_failsWithANegativeMargin() {
		// A 50 psi static / 20 psi residual at 100 gpm supply cannot serve a 40 gpm sprinkler
		// that needs 51 psi at its head.
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.EXTRA_GROUP_2, 0.40, 2500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 100.0)),
				List.of(segment("S1", "RISER", "2", 10.0, 0.0)),
				new SupplyCurve(50.0, 20.0, 100.0), List.of());

		SprinklerHydraulicResult result = calculator.calculate(input);

		assertFalse(result.passes());
		assertTrue(result.safetyMarginPsi() < 0, "margin was " + result.safetyMarginPsi());
	}

	@Test
	void supplyCurveSamples_includeTheDemandPointAndTheTestPoints() {
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(segment("S1", "RISER", "2", 10.0, 0.0)),
				SUPPLY, List.of());

		SprinklerHydraulicResult result = calculator.calculate(input);

		assertTrue(result.supplyCurve().stream().anyMatch(point -> point.flowGpm() == 0.0
				&& point.availablePressurePsi() == 80.0));
		assertTrue(result.supplyCurve().stream().anyMatch(point -> point.flowGpm() == 1000.0
				&& Math.abs(point.availablePressurePsi() - 60.0) < 1e-9));
		assertTrue(result.supplyCurve().stream()
						.anyMatch(point -> Math.abs(point.flowGpm() - result.totalDemandFlowGpm()) < 1e-9),
				"the demand point should be sampled too");
	}

	private static SegmentFlowRow row(SprinklerHydraulicResult result, String from, String to) {
		return result.segments().stream()
				.filter(candidate -> candidate.fromNodeId().equals(from) && candidate.toNodeId().equals(to))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no row for " + from + "->" + to + " in " + result.segments()));
	}

	@Test
	void everyListedSprinklerReportsItsOwnFlowAndPressure() {
		// The hand-calculated one-sprinkler case: K 5.6 at the 7 psi floor discharges 14.8162 gpm,
		// and its share of the design density (0.10 * 130 ft2) is 13 gpm.
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("S1", 5.6, 130.0)),
				List.of(segment("S1", "RISER", "2", 10.0, 0.0)),
				SUPPLY, List.of());

		SprinklerHydraulicResult result = calculator.calculate(input);

		assertEquals(1, result.sprinklers().size());
		SprinklerFlowRow row = result.sprinklers().get(0);
		assertEquals("S1", row.nodeId());
		assertEquals(5.6, row.kFactor(), 1e-12);
		assertEquals(14.8162073, row.flowGpm(), 1e-6);
		assertEquals(7.0, row.pressurePsi(), 1e-9);
		assertEquals(13.0, row.minimumFlowGpm(), 1e-9);
	}

	@Test
	void sprinklerFlowsAddUpToTheDemand_andNoSprinklerIsBelowItsMinimum() {
		// Three sprinklers on a line, flows differing because pressure builds up along the pipe.
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.ORDINARY_GROUP_1, 0.15, 1500.0, 0.0, null, 7.0,
				List.of(
						new RemoteAreaSprinkler("S1", 5.6, 130.0),
						new RemoteAreaSprinkler("S2", 5.6, 130.0),
						new RemoteAreaSprinkler("S3", 5.6, 130.0)),
				List.of(
						segment("S1", "S2", "1", 10.0, 0.0),
						segment("S2", "S3", "1", 10.0, 0.0),
						segment("S3", "RISER", "2", 10.0, 0.0)),
				SUPPLY, List.of());

		SprinklerHydraulicResult result = calculator.calculate(input);

		assertEquals(List.of("S1", "S2", "S3"), result.sprinklers().stream().map(SprinklerFlowRow::nodeId).toList());
		double sum = 0;
		for (SprinklerFlowRow row : result.sprinklers()) {
			sum += row.flowGpm();
			assertTrue(row.flowGpm() >= row.minimumFlowGpm() - 1e-9, row.nodeId() + " below its minimum");
			// Q = K sqrt(P): the pressure reported is the one that flow corresponds to.
			assertEquals(row.flowGpm(), row.kFactor() * Math.sqrt(row.pressurePsi()), 1e-9);
		}
		assertEquals(result.totalDemandFlowGpm(), sum, 1e-9);
		// Only the most remote is at its minimum; pressure builds, so the next ones flow more.
		assertEquals(19.5, result.sprinklers().get(0).flowGpm(), 1e-9);
		assertTrue(result.sprinklers().get(1).flowGpm() > result.sprinklers().get(0).flowGpm());
		assertTrue(result.sprinklers().get(2).flowGpm() > result.sprinklers().get(1).flowGpm());
	}

	@Test
	void aBalancedBranchsSprinklersAreReportedAtTheScaledFlow() {
		// Branch B runs longer pipe, so it arrives higher and governs; branch A is scaled up. A's
		// sprinkler must then be reported at the scaled flow, not at the one it was first worked at.
		SprinklerHydraulicInput input = new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinkler("A1", 5.6, 130.0), new RemoteAreaSprinkler("B1", 5.6, 130.0)),
				List.of(
						segment("A1", "J", "1", 10.0, 0.0),
						segment("B1", "J", "1", 20.0, 0.0),
						segment("J", "RISER", "2", 10.0, 0.0)),
				SUPPLY, List.of());

		SprinklerHydraulicResult result = calculator.calculate(input);

		SprinklerFlowRow a = result.sprinklers().stream().filter(r -> r.nodeId().equals("A1")).findFirst().orElseThrow();
		SprinklerFlowRow b = result.sprinklers().stream().filter(r -> r.nodeId().equals("B1")).findFirst().orElseThrow();
		assertEquals(14.8162073, b.flowGpm(), 1e-6);
		// 14.8162 * sqrt(8.495324 / 7.747662) = 15.514639 gpm, as in junctionBalancing above.
		assertEquals(15.514639, a.flowGpm(), 1e-5);
		assertEquals(a.flowGpm(), a.kFactor() * Math.sqrt(a.pressurePsi()), 1e-9);
		assertEquals(result.totalDemandFlowGpm(), a.flowGpm() + b.flowGpm(), 1e-9);
	}
}
