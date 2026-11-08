package com.renzoproject.calc.core.electrical.conduitfill;

import com.renzoproject.calc.core.electrical.reference.ConduitType;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConduitFillCalculatorTest {

	private static final double DELTA = 1e-6;

	private final ConduitFillCalculator calculator = new ConduitFillCalculator();

	@Test
	void threeThhn100InEmt_matchesHandCalculatedSizeAndFillPercent() {
		// THHN "100" area = 208.7mm2 x3 = 626.1mm2. 3 conductors -> 40% rule, over2Wires40 column.
		// EMT over2Wires40: 15->78, 20->137, 25->222, 32->387, 40->526, 50->866 (first >= 626.1).
		ConduitFillInput input = new ConduitFillInput(
				List.of(new ConductorFillEntry(InsulationType.THHN, "100", 3)), ConduitType.EMT);

		ConduitFillResult result = calculator.calculate(input);

		double expectedArea = 208.7 * 3;
		double expectedFillPercent = (expectedArea / 2165.0) * 100.0;

		assertEquals("50", result.recommendedTradeSizeMm());
		assertEquals(expectedArea, result.totalConductorAreaMm2(), DELTA);
		assertEquals(3, result.totalConductorCount());
		assertEquals(40.0, result.allowedFillPercent(), DELTA);
		assertEquals(expectedFillPercent, result.actualFillPercentAtRecommendedSize(), DELTA);
		assertFalse(result.requiresMultipleConduits());
	}

	@Test
	void aNipple_usesThe60PercentColumn_andTakesASmallerSize() {
		// THHN 100 mm2 x3 = 626.1 mm2. As a run, EMT needs 50 mm (40 % column 866). As a nipple of not more than
		// 600 mm, PEC Chapter 10 note (4) allows 60 %: EMT 60 % column 15->118, 20->206, 25->333, 32->581, 40->788.
		var conductors = List.of(new ConductorFillEntry(InsulationType.THHN, "100", 3));

		ConduitFillResult nipple = calculator.calculate(new ConduitFillInput(conductors, ConduitType.EMT, true));
		ConduitFillResult run = calculator.calculate(new ConduitFillInput(conductors, ConduitType.EMT));

		assertEquals("40", nipple.recommendedTradeSizeMm());
		assertEquals(60.0, nipple.allowedFillPercent(), DELTA);
		assertEquals(626.1 / 1314.0 * 100.0, nipple.actualFillPercentAtRecommendedSize(), DELTA);
		assertTrue(nipple.nippleFill());
		assertEquals("50", run.recommendedTradeSizeMm());
		assertEquals(40.0, run.allowedFillPercent(), DELTA);
		assertFalse(run.nippleFill());
	}

	@Test
	void aNipple_takes60PercentForOneAndTwoConductorsToo() {
		// Two THHN 22 mm2 = 105.6 mm2: a run takes EMT 20 mm (31 % column 106); a nipple fits EMT 15 mm (60 % column 118).
		var two = List.of(new ConductorFillEntry(InsulationType.THHN, "22", 2));
		assertEquals("20", calculator.calculate(new ConduitFillInput(two, ConduitType.EMT)).recommendedTradeSizeMm());
		ConduitFillResult nippleTwo = calculator.calculate(new ConduitFillInput(two, ConduitType.EMT, true));
		assertEquals("15", nippleTwo.recommendedTradeSizeMm());
		assertEquals(60.0, nippleTwo.allowedFillPercent(), DELTA);

		// One conductor is 53 % as a run and still 60 % as a nipple.
		var one = List.of(new ConductorFillEntry(InsulationType.THHN, "14", 1));
		assertEquals(53.0, calculator.calculate(new ConduitFillInput(one, ConduitType.EMT)).allowedFillPercent(), DELTA);
		assertEquals(60.0, calculator.calculate(new ConduitFillInput(one, ConduitType.EMT, true)).allowedFillPercent(), DELTA);
	}

	@Test
	void aNipple_hasNoPullEaseAdvisory_becauseItIsNotPulledThrough() {
		// 47.6 % fill is far above the 25 % pull-ease threshold, which would flag a run.
		var conductors = List.of(new ConductorFillEntry(InsulationType.THHN, "100", 3));

		ConduitFillResult nipple = calculator.calculate(new ConduitFillInput(conductors, ConduitType.EMT, true));
		ConduitFillResult run = calculator.calculate(new ConduitFillInput(conductors, ConduitType.EMT));

		assertFalse(nipple.practicalFillAdvisory().mayBeDifficultToPull());
		assertNull(nipple.practicalFillAdvisory().note());
		assertTrue(run.practicalFillAdvisory().mayBeDifficultToPull());
	}

	@Test
	void aNipple_stillNeedsMultipleConduitsWhenNothingFits() {
		var tooMany = List.of(new ConductorFillEntry(InsulationType.THHN, "500", 40));

		ConduitFillResult result = calculator.calculate(new ConduitFillInput(tooMany, ConduitType.EMT, true));

		assertTrue(result.requiresMultipleConduits());
		assertTrue(result.nippleFill());
		assertNull(result.recommendedTradeSizeMm());
	}

	@Test
	void oneConductor_usesOneWire53PercentColumn() {
		// THHN "14" area = 33.2mm2. 1 conductor -> 53% rule, oneWire53 column.
		// EMT oneWire53: 15->104 (first >= 33.2).
		ConduitFillInput input = new ConduitFillInput(
				List.of(new ConductorFillEntry(InsulationType.THHN, "14", 1)), ConduitType.EMT);

		ConduitFillResult result = calculator.calculate(input);

		double expectedFillPercent = (33.2 / 196.0) * 100.0;

		assertEquals("15", result.recommendedTradeSizeMm());
		assertEquals(33.2, result.totalConductorAreaMm2(), DELTA);
		assertEquals(1, result.totalConductorCount());
		assertEquals(53.0, result.allowedFillPercent(), DELTA);
		assertEquals(expectedFillPercent, result.actualFillPercentAtRecommendedSize(), DELTA);
	}

	@Test
	void twoConductors_usesTwoWires53PercentColumn() {
		// THHN "22" area = 52.8mm2 x2 = 105.6mm2. 2 conductors -> 31% rule, twoWires53 column.
		// EMT twoWires53: 15->61, 20->106 (first >= 105.6).
		ConduitFillInput input = new ConduitFillInput(
				List.of(new ConductorFillEntry(InsulationType.THHN, "22", 2)), ConduitType.EMT);

		ConduitFillResult result = calculator.calculate(input);

		double expectedArea = 52.8 * 2;
		double expectedFillPercent = (expectedArea / 343.0) * 100.0;

		assertEquals("20", result.recommendedTradeSizeMm());
		assertEquals(expectedArea, result.totalConductorAreaMm2(), DELTA);
		assertEquals(2, result.totalConductorCount());
		assertEquals(31.0, result.allowedFillPercent(), DELTA);
		assertEquals(expectedFillPercent, result.actualFillPercentAtRecommendedSize(), DELTA);
	}

	@Test
	void requiresLargestAvailableSizeForConduitType() {
		// THHN "500" area = 870.9mm2. LFNC-A oneWire53: ...32->513, 40->690, 50->1143 (its largest size).
		ConduitFillInput input = new ConduitFillInput(
				List.of(new ConductorFillEntry(InsulationType.THHN, "500", 1)), ConduitType.LFNC_A);

		ConduitFillResult result = calculator.calculate(input);

		double expectedFillPercent = (870.9 / 2157.0) * 100.0;

		assertEquals("50", result.recommendedTradeSizeMm());
		assertEquals(expectedFillPercent, result.actualFillPercentAtRecommendedSize(), DELTA);
		assertFalse(result.requiresMultipleConduits());
	}

	@Test
	void noSizeFits_returnsNullSizeAndRequiresMultipleConduitsTrue() {
		// 50 x THHN "500" (870.9mm2 each) = 43545mm2, far beyond FMC's largest entry (100mm,
		// over2Wires40 = 3243mm2).
		ConduitFillInput input = new ConduitFillInput(
				List.of(new ConductorFillEntry(InsulationType.THHN, "500", 50)), ConduitType.FMC);

		ConduitFillResult result = calculator.calculate(input);

		assertNull(result.recommendedTradeSizeMm());
		assertNull(result.actualFillPercentAtRecommendedSize());
		assertTrue(result.requiresMultipleConduits());
		assertEquals(870.9 * 50, result.totalConductorAreaMm2(), DELTA);
		assertNull(result.practicalFillAdvisory());
	}

	@Test
	void mixedInsulationTypesAndSizes_sumsCorrectly() {
		// 2x THHN "100" (208.7 each) + 1x THWN "60" (143.1) = 560.5mm2. 3 conductors -> 40% rule.
		// RMC over2Wires40: ...40->533, 50->879 (first >= 560.5).
		ConduitFillInput input = new ConduitFillInput(
				List.of(
						new ConductorFillEntry(InsulationType.THHN, "100", 2),
						new ConductorFillEntry(InsulationType.THWN, "60", 1)),
				ConduitType.RMC);

		ConduitFillResult result = calculator.calculate(input);

		double expectedArea = (208.7 * 2) + (143.1 * 1);
		double expectedFillPercent = (expectedArea / 2198.0) * 100.0;

		assertEquals("50", result.recommendedTradeSizeMm());
		assertEquals(expectedArea, result.totalConductorAreaMm2(), DELTA);
		assertEquals(3, result.totalConductorCount());
		assertEquals(expectedFillPercent, result.actualFillPercentAtRecommendedSize(), DELTA);
	}

	@Test
	void fillWellUnder25Percent_practicalAdvisoryNotDifficult() {
		// TF "1.25" area = 7.1mm2 x4 = 28.4mm2. 4 conductors -> 40% rule, over2Wires40 column.
		// PVC_SCHEDULE_40_HDPE 15mm: over2Wires40=74 (28.4 fits), totalArea100=184.
		// actualFillPercent = 28.4/184*100 ~= 15.43%, well under the 25% practical threshold.
		ConduitFillInput input = new ConduitFillInput(
				List.of(new ConductorFillEntry(InsulationType.TF, "1.25", 4)), ConduitType.PVC_SCHEDULE_40_HDPE);

		ConduitFillResult result = calculator.calculate(input);

		assertEquals("15", result.recommendedTradeSizeMm());
		assertTrue(result.actualFillPercentAtRecommendedSize() < PracticalFillAdvisory.PRACTICAL_PULL_THRESHOLD_PERCENT);
		assertFalse(result.practicalFillAdvisory().mayBeDifficultToPull());
		assertNull(result.practicalFillAdvisory().note());
	}

	@Test
	void fillOver25PercentButLegal_practicalAdvisoryFlagsDifficult() {
		// TF "1.25" area = 7.1mm2 x7 = 49.7mm2. Still fits 15mm PVC Sch 40 (over2Wires40=74,
		// so still legal under the 40% rule), but actualFillPercent = 49.7/184*100 ~= 27.01%,
		// over the 25% practical threshold even though it's within the legal limit.
		ConduitFillInput input = new ConduitFillInput(
				List.of(new ConductorFillEntry(InsulationType.TF, "1.25", 7)), ConduitType.PVC_SCHEDULE_40_HDPE);

		ConduitFillResult result = calculator.calculate(input);

		assertEquals("15", result.recommendedTradeSizeMm());
		assertTrue(result.actualFillPercentAtRecommendedSize() > PracticalFillAdvisory.PRACTICAL_PULL_THRESHOLD_PERCENT);
		assertTrue(result.actualFillPercentAtRecommendedSize() <= result.allowedFillPercent());
		assertTrue(result.practicalFillAdvisory().mayBeDifficultToPull());
		assertNotNull(result.practicalFillAdvisory().note());
		assertFalse(result.practicalFillAdvisory().note().isEmpty());
	}

}
