package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.JsonPipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityCalculator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Branches declared identical to another, against the Everstrong hydraulic calculation workbook
 * (Production Office sheet) and against the same network listed out in full.
 *
 * <p>The workbook is three branch lines of four sprinklers on a 3" cross main, then a riser:
 * <pre>
 *   BL-1  A: 92.095 gpm @ 30.287 psi        (worked from the first sprinkler)
 *   BL-2  B: 92.416 gpm @ 30.499 psi        = BL-1 * sqrt(30.499 / 30.287)
 *   BL-3  C: 93.568 gpm @ 31.264 psi        = BL-2 * sqrt(31.264 / 30.499)
 *   top of riser 278.08 gpm @ 41.07 psi, bottom 77.01 psi, 478.08 gpm with the 200 gpm hose
 * </pre>
 * The workbook looks inside diameters up to two decimals (1.05, 1.38, 3.07, 4.03); this engine uses
 * the published schedule-40 bores (1.049, 1.380, 3.068, 4.026). That is a few hundredths of a
 * percent on a bore, and about a tenth of a percent on the answers -- so the figures are compared
 * to within 0.3%, not to the digit.
 */
class IdenticalBranchTest {

	private static final double WORKBOOK_TOLERANCE = 0.003;

	private final JsonPipeDimensionResolver dimensionResolver = new JsonPipeDimensionResolver();
	private final SprinklerHydraulicCalculator calculator = new SprinklerHydraulicCalculator(
			dimensionResolver,
			new JsonFittingEquivalentLengthResolver(),
			new PipeVelocityCalculator(dimensionResolver));

	private static final SupplyCurve SUPPLY = new SupplyCurve(120.0, 100.0, 1000.0);

	/** A pipe run with its fittings entered as one explicit length, the way the workbook's sheet has them. */
	private static SprinklerPipeSegment pipe(
			String from, String to, String size, double lengthFt, double fittingsFt, double elevationFt) {
		List<SegmentFitting> fittings =
				fittingsFt > 0 ? List.of(new SegmentFitting(null, 1, fittingsFt)) : List.of();
		return new SprinklerPipeSegment(from, to, "BI", "SCH40", size, 120.0, lengthFt, elevationFt, fittings);
	}

	/** One branch line of the workbook: four sprinklers and the pipe after each, ending at the junction. */
	private static void addBranch(
			String prefix, String junction, List<RemoteAreaSprinkler> sprinklers, List<SprinklerPipeSegment> segments) {
		for (int i = 1; i <= 4; i++) {
			sprinklers.add(new RemoteAreaSprinkler(prefix + "-S" + i, 5.6, 130.0));
		}
		segments.add(pipe(prefix + "-S1", prefix + "-S2", "1", 10, 2, 0));
		segments.add(pipe(prefix + "-S2", prefix + "-S3", "1", 8, 5, 0));
		segments.add(pipe(prefix + "-S3", prefix + "-S4", "1-1/4", 10, 2, 0));
		segments.add(pipe(prefix + "-S4", junction, "1-1/4", 10, 2, 0));
	}

	private static void addMainAndRiser(List<SprinklerPipeSegment> segments) {
		segments.add(pipe("A", "B", "3", 10, 8, 0));
		segments.add(pipe("B", "C", "3", 10, 8, 0));
		segments.add(pipe("C", "TOR", "3", 100, 8, 0));
		// The workbook's riser reads "lgth 246, ftg 13, elev 20" and totals all three: 279 ft of pipe
		// for friction, plus the 20 ft rise for static pressure. Pipe length here is the centerline,
		// vertical run included, so the workbook's 246 + 20 goes in as 266.
		segments.add(pipe("TOR", "R2", "3", 266, 13, 20));
		segments.add(pipe("R2", "BOR", "4", 50, 31, 0));
	}

	private SprinklerHydraulicResult workbookWithIdenticalBranches() {
		List<RemoteAreaSprinkler> sprinklers = new ArrayList<>();
		List<SprinklerPipeSegment> segments = new ArrayList<>();
		addBranch("BL1", "A", sprinklers, segments);
		addMainAndRiser(segments);
		return calculator.calculate(input(
				sprinklers,
				segments,
				List.of(new IdenticalBranch("B", "BL1-S4"), new IdenticalBranch("C", "BL1-S4"))));
	}

	private static SprinklerHydraulicInput input(
			List<RemoteAreaSprinkler> sprinklers, List<SprinklerPipeSegment> segments, List<IdenticalBranch> copies) {
		return new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.ORDINARY_GROUP_1, 0.15, 1500.0, 200.0, "BOR", 7.0,
				sprinklers, segments, SUPPLY, List.of(), copies);
	}

	private static SegmentFlowRow row(SprinklerHydraulicResult result, String from, String to) {
		return result.segments().stream()
				.filter(r -> r.fromNodeId().equals(from) && r.toNodeId().equals(to))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no row " + from + "->" + to + " in " + result.segments()));
	}

	private static void assertWithin(String what, double workbook, double actual) {
		assertEquals(workbook, actual, Math.abs(workbook) * WORKBOOK_TOLERANCE, what);
	}

	@Test
	void everstrongWorkbook_branchesTwoAndThree_matchTheSheet() {
		SprinklerHydraulicResult result = workbookWithIdenticalBranches();

		// BL-1, listed: the cross main A->B carries its 92.095 gpm, arriving at B at 30.499 psi.
		SegmentFlowRow aToB = row(result, "A", "B");
		assertWithin("BL-1 flow", 92.095, aToB.flowGpm());
		assertWithin("pressure at B", 30.499, aToB.endPressurePsi());

		// B->C carries BL-1 + BL-2 = 184.512 gpm, arriving at C at 31.264 psi.
		SegmentFlowRow bToC = row(result, "B", "C");
		assertWithin("BL-1 + BL-2", 184.512, bToC.flowGpm());
		assertWithin("pressure at C", 31.264, bToC.endPressurePsi());

		// C->TOR carries all three, 278.08 gpm, arriving at 41.07 psi.
		SegmentFlowRow cToTop = row(result, "C", "TOR");
		assertWithin("three branches", 278.080, cToTop.flowGpm());
		assertWithin("top of riser", 41.068, cToTop.endPressurePsi());

		// Riser: 75.06 psi at the change of size, 77.01 psi at the bottom.
		assertWithin("bottom of riser", 77.011, row(result, "R2", "BOR").endPressurePsi());

		// Hose stream 200 gpm is added at the bottom of the riser: 478.08 gpm at 77.01 psi.
		assertWithin("demand with hose", 478.080, result.totalDemandFlowGpm());
		assertWithin("required pressure", 77.011, result.requiredPressureAtSupplyPsi());
	}

	@Test
	void everstrongWorkbook_eachIdenticalBranchIsReportedAsOneBranchLevelLine() {
		SprinklerHydraulicResult result = workbookWithIdenticalBranches();

		assertEquals(2, result.identicalBranches().size());

		IdenticalBranchRow bl2 = result.identicalBranches().get(0);
		assertEquals("B", bl2.joinNodeId());
		assertEquals("BL1-S4", bl2.referenceNodeId());
		assertEquals("A", bl2.referenceJunctionNodeId());
		assertEquals(4, bl2.sprinklerCount());
		assertWithin("BL-2 flow", 92.416, bl2.flowGpm());
		assertWithin("BL-2 junction pressure", 30.499, bl2.junctionPressurePsi());

		IdenticalBranchRow bl3 = result.identicalBranches().get(1);
		assertEquals("C", bl3.joinNodeId());
		assertWithin("BL-3 flow", 93.568, bl3.flowGpm());
		assertWithin("BL-3 junction pressure", 31.264, bl3.junctionPressurePsi());
	}

	@Test
	void theLineStatesTheFormula_QEqualsQrefTimesRootOfPressureRatio() {
		SprinklerHydraulicResult result = workbookWithIdenticalBranches();

		for (IdenticalBranchRow line : result.identicalBranches()) {
			double factor = Math.sqrt(line.junctionPressurePsi() / line.referencePressurePsi());
			assertEquals(factor, line.scaleFactor(), 1e-12);
			assertEquals(line.referenceFlowGpm() * factor, line.flowGpm(), 1e-9);
		}

		// Both copies scale from the reference as worked from its own first sprinkler, not from
		// each other: the reference figures are the same on both lines.
		IdenticalBranchRow first = result.identicalBranches().get(0);
		IdenticalBranchRow second = result.identicalBranches().get(1);
		assertEquals(first.referenceFlowGpm(), second.referenceFlowGpm(), 1e-9);
		assertEquals(first.referencePressurePsi(), second.referencePressurePsi(), 1e-9);
	}

	@Test
	void anIdenticalBranch_givesTheSameAnswerAsTheSameBranchListedOutInFull() {
		// The shortcut must be exactly what it stands in for: three branches listed sprinkler by
		// sprinkler against one listed and two declared.
		List<RemoteAreaSprinkler> sprinklers = new ArrayList<>();
		List<SprinklerPipeSegment> segments = new ArrayList<>();
		addBranch("BL1", "A", sprinklers, segments);
		addBranch("BL2", "B", sprinklers, segments);
		addBranch("BL3", "C", sprinklers, segments);
		addMainAndRiser(segments);
		SprinklerHydraulicResult listed = calculator.calculate(input(sprinklers, segments, List.of()));

		SprinklerHydraulicResult declared = workbookWithIdenticalBranches();

		assertEquals(listed.totalDemandFlowGpm(), declared.totalDemandFlowGpm(), 1e-9);
		assertEquals(listed.requiredPressureAtSupplyPsi(), declared.requiredPressureAtSupplyPsi(), 1e-9);
		for (String[] pair : new String[][] {{"A", "B"}, {"B", "C"}, {"C", "TOR"}, {"TOR", "R2"}, {"R2", "BOR"}}) {
			SegmentFlowRow expected = row(listed, pair[0], pair[1]);
			SegmentFlowRow actual = row(declared, pair[0], pair[1]);
			assertEquals(expected.flowGpm(), actual.flowGpm(), 1e-9, pair[0] + "->" + pair[1] + " flow");
			assertEquals(expected.endPressurePsi(), actual.endPressurePsi(), 1e-9, pair[0] + "->" + pair[1]);
		}
	}

	@Test
	void theCopiesPipesAreNotListedAgain_theirFlowIsStillCounted() {
		SprinklerHydraulicResult result = workbookWithIdenticalBranches();

		// Four listed sprinkler pipes plus the cross mains and riser; nothing named for BL-2 or BL-3.
		assertEquals(4 + 3 + 2, result.segments().size());
		assertTrue(result.segments().stream().noneMatch(r -> r.fromNodeId().startsWith("BL2")));
		assertTrue(result.segments().stream().noneMatch(r -> r.fromNodeId().startsWith("BL3")));
	}

	@Test
	void eachCopyKeepsItsOwnDistance_aLongerCrossMainCostsMorePressure() {
		// Same network, but the cross main between B and C is stretched. BL-3 then sits at a higher
		// pressure and is scaled harder; BL-2 is unaffected.
		List<RemoteAreaSprinkler> sprinklers = new ArrayList<>();
		List<SprinklerPipeSegment> segments = new ArrayList<>();
		addBranch("BL1", "A", sprinklers, segments);
		segments.add(pipe("A", "B", "3", 10, 8, 0));
		segments.add(pipe("B", "C", "3", 60, 8, 0));
		segments.add(pipe("C", "TOR", "3", 100, 8, 0));
		segments.add(pipe("TOR", "R2", "3", 266, 13, 20));
		segments.add(pipe("R2", "BOR", "4", 50, 31, 0));
		SprinklerHydraulicResult stretched = calculator.calculate(input(
				sprinklers, segments, List.of(new IdenticalBranch("B", "BL1-S4"), new IdenticalBranch("C", "BL1-S4"))));
		SprinklerHydraulicResult workbook = workbookWithIdenticalBranches();

		assertEquals(
				workbook.identicalBranches().get(0).flowGpm(),
				stretched.identicalBranches().get(0).flowGpm(),
				1e-9);
		assertTrue(stretched.identicalBranches().get(1).flowGpm() > workbook.identicalBranches().get(1).flowGpm());
		assertTrue(stretched.requiredPressureAtSupplyPsi() > workbook.requiredPressureAtSupplyPsi());
	}

	@Test
	void aBranchCanRepeatAnotherThatIsItselfBalanced() {
		// BL-3 repeats BL-2 (not BL-1): the same answer, because the copy starts from the
		// reference's own result, not from whatever its junction scaled it to.
		List<RemoteAreaSprinkler> sprinklers = new ArrayList<>();
		List<SprinklerPipeSegment> segments = new ArrayList<>();
		addBranch("BL1", "A", sprinklers, segments);
		addBranch("BL2", "B", sprinklers, segments);
		addMainAndRiser(segments);
		SprinklerHydraulicResult result = calculator.calculate(input(
				sprinklers, segments, List.of(new IdenticalBranch("C", "BL2-S4"))));
		SprinklerHydraulicResult workbook = workbookWithIdenticalBranches();

		assertEquals(workbook.totalDemandFlowGpm(), result.totalDemandFlowGpm(), 1e-9);
		assertEquals(workbook.requiredPressureAtSupplyPsi(), result.requiredPressureAtSupplyPsi(), 1e-9);
	}

	@Test
	void withNoIdenticalBranches_theListIsEmpty() {
		List<RemoteAreaSprinkler> sprinklers = new ArrayList<>();
		List<SprinklerPipeSegment> segments = new ArrayList<>();
		addBranch("BL1", "A", sprinklers, segments);
		segments.add(pipe("A", "TOR", "3", 10, 8, 0));

		SprinklerHydraulicResult result = calculator.calculate(new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.ORDINARY_GROUP_1, 0.15, 1500.0, 0.0, null, 7.0,
				sprinklers, segments, SUPPLY, List.of()));

		assertTrue(result.identicalBranches().isEmpty());
	}

	@Test
	void aCopyWhosePipesRunHotWarnsAboutVelocity_atTheFlowItWasScaledTo() {
		// The branch's last run is undersized at 1", so ~92 gpm runs it past 20 fps. The reference
		// is listed and warns for itself; the copy has no rows of its own, so its warning has to
		// come from the copy being checked at the flow it was scaled to.
		List<RemoteAreaSprinkler> sprinklers = new ArrayList<>();
		List<SprinklerPipeSegment> segments = new ArrayList<>();
		addBranch("BL1", "A", sprinklers, segments);
		segments.set(3, pipe("BL1-S4", "A", "1", 10, 2, 0));
		segments.add(pipe("A", "B", "3", 10, 8, 0));
		segments.add(pipe("B", "TOR", "3", 10, 8, 0));
		SprinklerHydraulicResult result = calculator.calculate(new SprinklerHydraulicInput(
				SprinklerSystemType.WET, SprinklerHazard.ORDINARY_GROUP_1, 0.15, 1500.0, 0.0, null, 7.0,
				sprinklers, segments, SUPPLY, List.of(), List.of(new IdenticalBranch("B", "BL1-S4"))));

		assertTrue(
				result.warnings().stream().anyMatch(w -> w.contains("identical to it that joins at B")),
				"expected a velocity warning for the copy, got " + result.warnings());
	}

	@Test
	void rejectsAJunctionThatIsNotOnTheNetwork() {
		List<RemoteAreaSprinkler> sprinklers = new ArrayList<>();
		List<SprinklerPipeSegment> segments = new ArrayList<>();
		addBranch("BL1", "A", sprinklers, segments);
		segments.add(pipe("A", "TOR", "3", 10, 8, 0));

		CalculationException error = assertThrows(CalculationException.class, () -> calculator.calculate(
				new SprinklerHydraulicInput(
						SprinklerSystemType.WET, SprinklerHazard.ORDINARY_GROUP_1, 0.15, 1500.0, 0.0, null, 7.0,
						sprinklers, segments, SUPPLY, List.of(), List.of(new IdenticalBranch("NOWHERE", "BL1-S4")))));

		assertTrue(error.getMessage().contains("NOWHERE"), error.getMessage());
	}

	@Test
	void rejectsAReferenceWithNoPipeLeavingIt() {
		List<RemoteAreaSprinkler> sprinklers = new ArrayList<>();
		List<SprinklerPipeSegment> segments = new ArrayList<>();
		addBranch("BL1", "A", sprinklers, segments);
		segments.add(pipe("A", "TOR", "3", 10, 8, 0));

		// TOR is the supply: nothing leaves it, so there is no branch ending there to repeat.
		CalculationException error = assertThrows(CalculationException.class, () -> calculator.calculate(
				new SprinklerHydraulicInput(
						SprinklerSystemType.WET, SprinklerHazard.ORDINARY_GROUP_1, 0.15, 1500.0, 0.0, null, 7.0,
						sprinklers, segments, SUPPLY, List.of(), List.of(new IdenticalBranch("A", "TOR")))));

		assertTrue(error.getMessage().contains("no pipe leaving"), error.getMessage());
	}

	@Test
	void rejectsABranchThatWouldRepeatSomethingItIsPartOf() {
		List<RemoteAreaSprinkler> sprinklers = new ArrayList<>();
		List<SprinklerPipeSegment> segments = new ArrayList<>();
		addBranch("BL1", "A", sprinklers, segments);
		segments.add(pipe("A", "TOR", "3", 10, 8, 0));

		// Joining at A while repeating the branch that is fed through A.
		CalculationException error = assertThrows(CalculationException.class, () -> calculator.calculate(
				new SprinklerHydraulicInput(
						SprinklerSystemType.WET, SprinklerHazard.ORDINARY_GROUP_1, 0.15, 1500.0, 0.0, null, 7.0,
						sprinklers, segments, SUPPLY, List.of(), List.of(new IdenticalBranch("BL1-S2", "BL1-S4")))));

		assertTrue(error.getMessage().contains("fed through"), error.getMessage());
	}

	@Test
	void rejectsCopiesThatRepeatEachOtherInACircle() {
		// Two separate legs meeting at M. Each leg's junction is declared a copy of the other leg's
		// last pipe, so each needs the other worked out first. Neither declaration is wrong on its
		// own -- only together do they go round in a circle.
		List<RemoteAreaSprinkler> sprinklers = List.of(
				new RemoteAreaSprinkler("SX", 5.6, 130.0), new RemoteAreaSprinkler("SY", 5.6, 130.0));
		List<SprinklerPipeSegment> segments = List.of(
				pipe("SX", "J1", "1", 10, 0, 0),
				pipe("J1", "R2", "1-1/4", 10, 0, 0),
				pipe("R2", "M", "1-1/4", 10, 0, 0),
				pipe("SY", "J2", "1", 10, 0, 0),
				pipe("J2", "R1", "1-1/4", 10, 0, 0),
				pipe("R1", "M", "1-1/4", 10, 0, 0),
				pipe("M", "TOR", "2", 10, 0, 0));

		CalculationException error = assertThrows(CalculationException.class, () -> calculator.calculate(
				new SprinklerHydraulicInput(
						SprinklerSystemType.WET, SprinklerHazard.ORDINARY_GROUP_1, 0.15, 1500.0, 0.0, null, 7.0,
						sprinklers, segments, SUPPLY, List.of(),
						List.of(new IdenticalBranch("J1", "R1"), new IdenticalBranch("J2", "R2")))));

		assertTrue(error.getMessage().contains("circle"), error.getMessage());
	}

	@Test
	void aDeclarationNeedsBothNodes() {
		assertThrows(CalculationException.class, () -> new IdenticalBranch("", "BL1-S4"));
		assertThrows(CalculationException.class, () -> new IdenticalBranch("B", " "));
		assertThrows(CalculationException.class, () -> new IdenticalBranch("B", "B"));
	}

	@Test
	void theListedSprinklersAreReported_theCopiesAreOnlyTheirBranchLine() {
		SprinklerHydraulicResult result = workbookWithIdenticalBranches();

		// BL-1's four sprinklers only: the ones BL-2 and BL-3 stand for are not listed.
		assertEquals(
				List.of("BL1-S1", "BL1-S2", "BL1-S3", "BL1-S4"),
				result.sprinklers().stream().map(SprinklerFlowRow::nodeId).toList());

		// Listed sprinklers + what the copies carry + the hose stream is the whole demand.
		double listed = result.sprinklers().stream().mapToDouble(SprinklerFlowRow::flowGpm).sum();
		double copies = result.identicalBranches().stream().mapToDouble(IdenticalBranchRow::flowGpm).sum();
		assertEquals(result.totalDemandFlowGpm(), listed + copies + 200.0, 1e-9);

		// The first sprinkler of the whole network is the one held at the minimum: 0.15 * 130.
		assertEquals(19.5, result.sprinklers().get(0).flowGpm(), 1e-9);
		assertEquals(19.5, result.sprinklers().get(0).minimumFlowGpm(), 1e-9);
	}
}
