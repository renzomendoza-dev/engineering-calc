package com.renzoproject.calc.core.electrical.wiresizing;

import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConductorProperties;
import com.renzoproject.calc.core.electrical.reference.ConductorPropertiesResolver;
import com.renzoproject.calc.core.electrical.reference.ConductorSize;
import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropCalculator;
import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropInput;
import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropResult;
import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireSizingCalculatorTest {

	private static final double DELTA = 1e-6;

	private final WireSizingCalculator calculator = new WireSizingCalculator();

	@Test
	void standardConditions_matchesHandCalculatedSizeAndAmpacity() {
		// THHN copper -> 90C column. 30C ambient, 90C factor = 1.0 (26-30 row). count=2 -> no
		// adjustment. COPPER "2.0" @ 90C = 25A >= 15A required -> recommended immediately.
		WireSizingInput input = new WireSizingInput(
				15.0, false, 30.0, 2, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null);

		WireSizingResult result = calculator.calculate(input);

		assertEquals("2.0", result.recommendedSizeLabel());
		assertEquals(25.0, result.baseAmpacityAmps(), DELTA);
		assertEquals(1.0, result.tempCorrectionFactor(), DELTA);
		assertEquals(1.0, result.adjustmentFactor(), DELTA);
		assertEquals(25.0, result.deratedAmpacityAmps(), DELTA);
		assertEquals(15.0, result.requiredAmpacityAmps(), DELTA);
		assertNull(result.voltageDropCheckResult());
	}

	private WireSizingResult withDevice(double loadAmps, ConductorMaterial material, Double deviceAmps) {
		return calculator.calculate(new WireSizingInput(
				loadAmps, false, 30.0, 2, 1, InsulationType.THHN, material, 75, null, deviceAmps));
	}

	@Test
	void smallConductorLimit_isReportedForTheRecommendedSize_whenNoDeviceIsGiven() {
		// 20 A of THHN copper fits 2.0 mm2 by ampacity (25 A at 90 C). PEC 2.40.1.4(D)(3) caps its protection at 15 A.
		WireSizingResult result = withDevice(20.0, ConductorMaterial.COPPER, null);

		assertEquals("2.0", result.recommendedSizeLabel());
		assertEquals(15, result.maximumOvercurrentDeviceAmps());
		assertNull(result.overcurrentDeviceAmps());
		assertEquals("2.0", result.ampacitySizeLabel());
		assertFalse(result.sizeRaisedForOvercurrentLimit());
	}

	@Test
	void aDeviceAtTheLimit_keepsTheAmpacitySize() {
		WireSizingResult result = withDevice(12.0, ConductorMaterial.COPPER, 15.0);

		assertEquals("2.0", result.recommendedSizeLabel());
		assertFalse(result.sizeRaisedForOvercurrentLimit());
		assertEquals(15.0, result.overcurrentDeviceAmps(), DELTA);
	}

	@Test
	void aDeviceAboveTheLimit_raisesTheSizeToOneItMayProtect() {
		// The same 20 A load behind a 20 A breaker: 2.0 mm2 copper (limit 15 A) is passed over for 3.5 mm2 (limit 20 A).
		WireSizingResult result = withDevice(20.0, ConductorMaterial.COPPER, 20.0);

		assertEquals("3.5", result.recommendedSizeLabel());
		assertEquals("2.0", result.ampacitySizeLabel());
		assertTrue(result.sizeRaisedForOvercurrentLimit());
		assertEquals(20, result.maximumOvercurrentDeviceAmps());
		assertEquals(30.0, result.baseAmpacityAmps(), DELTA);
	}

	@Test
	void aLargerDevice_stepsUpThroughTheLimitsUntilTheCapsRunOut() {
		// A 25 A device is above the 3.5 mm2 limit (20 A) as well, so the next size is 5.5 mm2 (limit 30 A).
		assertEquals("5.5", withDevice(20.0, ConductorMaterial.COPPER, 25.0).recommendedSizeLabel());
		assertEquals(30, withDevice(20.0, ConductorMaterial.COPPER, 25.0).maximumOvercurrentDeviceAmps());
		// 40 A is above every small-conductor limit, so 8.0 mm2 upward, which the clause does not cap.
		WireSizingResult large = withDevice(20.0, ConductorMaterial.COPPER, 40.0);
		assertEquals("8.0", large.recommendedSizeLabel());
		assertNull(large.maximumOvercurrentDeviceAmps());
		assertTrue(large.sizeRaisedForOvercurrentLimit());
	}

	@Test
	void aluminum_hasItsOwnLimits() {
		// Aluminum starts at 3.5 mm2 (15 A limit); a 20 A device needs 5.5 mm2 (25 A limit).
		WireSizingResult atLimit = withDevice(15.0, ConductorMaterial.ALUMINUM, 15.0);
		assertEquals("3.5", atLimit.recommendedSizeLabel());
		assertEquals(15, atLimit.maximumOvercurrentDeviceAmps());
		WireSizingResult above = withDevice(15.0, ConductorMaterial.ALUMINUM, 20.0);
		assertEquals("5.5", above.recommendedSizeLabel());
		assertEquals("3.5", above.ampacitySizeLabel());
		assertEquals(25, above.maximumOvercurrentDeviceAmps());
	}

	@Test
	void aLoadThatNeedsAMediumSizeAnyway_isUnaffectedByTheDevice() {
		// 60 A of load is well past the small conductors: no limit applies and nothing is raised.
		WireSizingResult result = withDevice(60.0, ConductorMaterial.COPPER, 70.0);

		assertNull(result.maximumOvercurrentDeviceAmps());
		assertFalse(result.sizeRaisedForOvercurrentLimit());
		assertEquals(result.ampacitySizeLabel(), result.recommendedSizeLabel());
	}

	private WireSizingResult sevenConductors(double loadAmps, double ambientCelsius, Double deviceAmps, boolean nipple) {
		return calculator.calculate(new WireSizingInput(
				loadAmps, false, ambientCelsius, 7, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null, deviceAmps, nipple));
	}

	@Test
	void aNipple_takesNoConductorCountAdjustment() {
		// 24 A on seven current-carrying THHN copper conductors. As a run, 3.10.2.6(B)(3)(a) gives 70 %:
		// 3.5 mm2 (30 A x 0.7 = 21 A) is short, so 5.5 mm2. As a nipple of not more than 600 mm (PEC
		// Chapter 10, note (4)) the factor need not apply: 2.0 mm2 (25 A) carries it.
		WireSizingResult run = sevenConductors(24.0, 30.0, null, false);
		assertEquals("5.5", run.recommendedSizeLabel());
		assertEquals(0.7, run.adjustmentFactor(), DELTA);
		assertFalse(run.nipple());

		WireSizingResult nipple = sevenConductors(24.0, 30.0, null, true);
		assertEquals("2.0", nipple.recommendedSizeLabel());
		assertEquals(1.0, nipple.adjustmentFactor(), DELTA);
		assertEquals(25.0, nipple.deratedAmpacityAmps(), DELTA);
		assertTrue(nipple.nipple());
	}

	@Test
	void aNipple_stillTakesTheAmbientCorrectionAndTheSmallConductorLimit() {
		// At 40 C the 90 C column is corrected (0.91): 2.0 mm2 gives 22.75 A, short of 24 A, so 3.5 mm2.
		WireSizingResult hot = sevenConductors(24.0, 40.0, null, true);
		assertEquals(0.91, hot.tempCorrectionFactor(), DELTA);
		assertEquals(1.0, hot.adjustmentFactor(), DELTA);
		assertEquals("3.5", hot.recommendedSizeLabel());

		// Behind a 20 A breaker 2.0 mm2 copper (2.40.1.4(D) limit 15 A) is passed over, nipple or not.
		WireSizingResult behindBreaker = sevenConductors(15.0, 30.0, 20.0, true);
		assertEquals("2.0", behindBreaker.ampacitySizeLabel());
		assertEquals("3.5", behindBreaker.recommendedSizeLabel());
		assertTrue(behindBreaker.sizeRaisedForOvercurrentLimit());
	}

	private WireSizingResult protectedBy(double loadAmps, int parallelSets, Double deviceAmps, boolean multipleReceptacles) {
		return calculator.calculate(new WireSizingInput(loadAmps, false, 30.0, 3, parallelSets, InsulationType.THHN,
				ConductorMaterial.COPPER, 75, null, deviceAmps, false, multipleReceptacles));
	}

	@Test
	void theNextStandardRatingAboveTheAmpacity_mayProtectIt() {
		// 45 A of THHN copper: 5.5 mm2 (40 A) is short, 8.0 mm2 carries 55 A. 2.40.1.4(B) lets the next standard rating, 60 A, protect it.
		WireSizingResult result = protectedBy(45.0, 1, 60.0, false);

		assertEquals("8.0", result.recommendedSizeLabel());
		assertFalse(result.sizeRaisedForOvercurrentLimit());
		assertNull(result.overcurrentLimitClause());
		assertEquals(55.0, result.protectedAmpacityAmps(), DELTA);
		assertEquals(60, result.standardRatingLimitAmps());
	}

	@Test
	void aDeviceAboveTheNextStandardRating_raisesTheSize() {
		// A 70 A device is above 60 A, so 8.0 mm2 is passed over for 14 mm2 (75 A, next rating 80 A).
		WireSizingResult result = protectedBy(45.0, 1, 70.0, false);

		assertEquals("14", result.recommendedSizeLabel());
		assertEquals("8.0", result.ampacitySizeLabel());
		assertTrue(result.sizeRaisedForOvercurrentLimit());
		assertEquals("2.40.1.4(B)", result.overcurrentLimitClause());
		assertEquals(80, result.standardRatingLimitAmps());
		assertNull(result.maximumOvercurrentDeviceAmps());
	}

	@Test
	void aMultiReceptacleBranchCircuit_mayNotTakeTheNextRatingUp() {
		// 2.40.1.4(B)(1): 8.0 mm2 (55 A) may take only 50 A, so a 60 A device needs 14 mm2 (75 A, 70 A).
		WireSizingResult result = protectedBy(45.0, 1, 60.0, true);

		assertEquals("14", result.recommendedSizeLabel());
		assertEquals("2.40.1.4(B)", result.overcurrentLimitClause());
		assertEquals(70, result.standardRatingLimitAmps());
		assertTrue(result.multipleReceptacleBranchCircuit());
	}

	@Test
	void above800Amperes_theAmpacityMustReachTheDevice() {
		// 900 A in three parallel sets: 150 mm2 (320 A each, 960 A together) is enough for the load, but
		// a 1000 A device is over 800 A, so 2.40.1.4(C) wants 1000 A of ampacity: 175 mm2 (3 x 345 = 1035 A).
		WireSizingResult result = protectedBy(900.0, 3, 1000.0, false);

		assertEquals("150", result.ampacitySizeLabel());
		assertEquals("175", result.recommendedSizeLabel());
		assertEquals("2.40.1.4(C)", result.overcurrentLimitClause());
		assertEquals(1035.0, result.protectedAmpacityAmps(), DELTA);
		assertEquals(1000, result.standardRatingLimitAmps());
	}

	@Test
	void theSmallConductorLimit_isNamedWhenItIsWhatRaisesTheSize() {
		WireSizingResult result = withDevice(20.0, ConductorMaterial.COPPER, 20.0);

		assertEquals("2.40.1.4(D)", result.overcurrentLimitClause());
	}

	@Test
	void withoutADevice_theStandardRatingLimitIsStillReported() {
		// 2.0 mm2 carries 25 A, itself a standard rating.
		WireSizingResult result = protectedBy(15.0, 1, null, false);

		assertEquals("2.0", result.recommendedSizeLabel());
		assertEquals(25, result.standardRatingLimitAmps());
		assertNull(result.overcurrentLimitClause());
		assertFalse(result.multipleReceptacleBranchCircuit());
	}

	@Test
	void theTenArgumentInput_isNotANipple() {
		WireSizingInput input = new WireSizingInput(15.0, false, 30.0, 7, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null, null);

		assertFalse(input.nipple());
		assertEquals(0.7, calculator.calculate(input).adjustmentFactor(), DELTA);
	}

	@Test
	void theInputWithoutADevice_isTheOldNineArgumentConstructor() {
		WireSizingInput input = new WireSizingInput(15.0, false, 30.0, 2, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null);

		assertNull(input.overcurrentDeviceAmps());
		assertThrows(CalculationException.class, () -> new WireSizingInput(
				15.0, false, 30.0, 2, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null, 0.0));
		assertThrows(CalculationException.class, () -> new WireSizingInput(
				15.0, false, 30.0, 2, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null, -20.0));
	}

	@Test
	void continuousLoad_appliesOneTwentyFivePercentFactor() {
		WireSizingInput input = new WireSizingInput(
				15.0, true, 30.0, 2, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null);

		WireSizingResult result = calculator.calculate(input);

		assertEquals(18.75, result.requiredAmpacityAmps(), DELTA);
		assertEquals("2.0", result.recommendedSizeLabel());
		assertEquals(25.0, result.deratedAmpacityAmps(), DELTA);
	}

	@Test
	void moreThanThreeConductors_adjustmentFactorPushesToLargerSize() {
		// Baseline: 1 current-carrying conductor, no adjustment -> "2.0" (25A) covers 20A.
		WireSizingInput baselineInput = new WireSizingInput(
				20.0, false, 30.0, 1, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null);
		WireSizingResult baseline = calculator.calculate(baselineInput);
		assertEquals("2.0", baseline.recommendedSizeLabel());
		assertEquals(1.0, baseline.adjustmentFactor(), DELTA);

		// 8 current-carrying conductors -> 70% adjustment. "2.0" derates to 25*0.7=17.5 < 20A,
		// so it must step up to "3.5" (30A @ 90C, derates to 21A >= 20A).
		WireSizingInput input = new WireSizingInput(
				20.0, false, 30.0, 8, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null);
		WireSizingResult result = calculator.calculate(input);

		assertEquals(0.70, result.adjustmentFactor(), DELTA);
		assertEquals("3.5", result.recommendedSizeLabel());
		assertEquals(30.0, result.baseAmpacityAmps(), DELTA);
		assertEquals(21.0, result.deratedAmpacityAmps(), DELTA);
	}

	@Test
	void elevatedAmbientTemp_correctionFactorPushesToLargerSize() {
		// Baseline at 30C ambient (factor 1.0): "2.0" (25A) covers 20A.
		WireSizingInput baselineInput = new WireSizingInput(
				20.0, false, 30.0, 2, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null);
		WireSizingResult baseline = calculator.calculate(baselineInput);
		assertEquals("2.0", baseline.recommendedSizeLabel());

		// At 55C ambient, 90C factor = 0.76 (51-55 row). "2.0" derates to 25*0.76=19 < 20A, so
		// it must step up to "3.5" (30A @ 90C, derates to 30*0.76=22.8 >= 20A).
		WireSizingInput input = new WireSizingInput(
				20.0, false, 55.0, 2, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null);
		WireSizingResult result = calculator.calculate(input);

		assertEquals(0.76, result.tempCorrectionFactor(), DELTA);
		assertEquals("3.5", result.recommendedSizeLabel());
		assertEquals(30.0, result.baseAmpacityAmps(), DELTA);
		assertEquals(22.8, result.deratedAmpacityAmps(), DELTA);
	}

	@Test
	void terminationRating_failsWhenSixtyCColumnCannotCarryRequiredLoad() {
		// THHN copper is 90C-rated: "2.0" @ 90C = 25A, derated (30C/count<=3, no correction) =
		// 25A >= 20A required -> recommended "2.0". But its 60C-column ampacity is only 15A,
		// which — under the SAME derating — cannot carry the 20A load: a common real-world
		// mismatch when terminals/lugs are only rated for 60C.
		WireSizingInput input = new WireSizingInput(
				20.0, false, 30.0, 1, 1, InsulationType.THHN, ConductorMaterial.COPPER, 60, null);

		WireSizingResult result = calculator.calculate(input);

		assertEquals("2.0", result.recommendedSizeLabel());
		assertFalse(result.meetsTerminationRating());
	}

	@Test
	void voltageDropCheck_ampacityBasedSizeAlsoPasses_upsizedRecommendationNull() throws Exception {
		WireSizingInput input = new WireSizingInput(
				15.0, false, 30.0, 2, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75,
				new VoltageDropCheckRequest(CircuitType.SINGLE_PHASE_AC, 5.0, 0.9, 230.0, ConduitMaterial.PVC, 1));

		WireSizingResult result = calculator.calculate(input);

		assertEquals("2.0", result.recommendedSizeLabel());
		assertNotNull(result.voltageDropCheckResult());
		assertEquals("2.0", result.voltageDropCheckResult().sizeLabelChecked());
		assertFalse(result.voltageDropCheckResult().exceedsRecommendedLimit());
		assertNull(result.voltageDropCheckResult().upsizedRecommendation());

		double expectedPercent = independentVoltageDropPercentForSize2_0(15.0, 5.0, 0.9, 230.0);
		assertEquals(expectedPercent, result.voltageDropCheckResult().voltageDropPercent(), DELTA);
		assertTrue(expectedPercent < VoltageDropCalculator.RECOMMENDED_MAX_PERCENT);
	}

	@Test
	void voltageDropCheck_ampacityBasedSizeFails_upsizedRecommendationFoundAndVerified() throws Exception {
		// Same ampacity scenario as the passing case, but a 200m run pushes voltage drop at
		// "2.0" far past the 3% limit, forcing the calculator to walk up to a larger size.
		WireSizingInput input = new WireSizingInput(
				15.0, false, 30.0, 2, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75,
				new VoltageDropCheckRequest(CircuitType.SINGLE_PHASE_AC, 200.0, 0.9, 230.0, ConduitMaterial.PVC, 1));

		WireSizingResult result = calculator.calculate(input);

		assertEquals("2.0", result.recommendedSizeLabel());
		assertNotNull(result.voltageDropCheckResult());
		assertEquals("2.0", result.voltageDropCheckResult().sizeLabelChecked());
		assertTrue(result.voltageDropCheckResult().exceedsRecommendedLimit());
		assertNotNull(result.voltageDropCheckResult().upsizedRecommendation());

		double checkedPercent = independentVoltageDropPercentForSize2_0(15.0, 200.0, 0.9, 230.0);
		assertEquals(checkedPercent, result.voltageDropCheckResult().voltageDropPercent(), DELTA);
		assertTrue(checkedPercent > VoltageDropCalculator.RECOMMENDED_MAX_PERCENT);

		// Independently re-verify the upsized size actually passes voltage drop, using a fresh
		// resolver/calculator instance rather than trusting the production code path.
		String upsized = result.voltageDropCheckResult().upsizedRecommendation();
		ConductorPropertiesResolver resolver = new ConductorPropertiesResolver();
		VoltageDropCalculator vdCalculator = new VoltageDropCalculator();
		ConductorProperties properties = resolver.resolve(
				CircuitType.SINGLE_PHASE_AC, new ConductorSize(upsized, Double.parseDouble(upsized)), ConductorMaterial.COPPER, ConduitMaterial.PVC);
		VoltageDropResult verifyResult = vdCalculator.calculate(new VoltageDropInput(
				CircuitType.SINGLE_PHASE_AC, 15.0, 200.0,
				properties.resistanceOhmsPerMeter(), properties.reactanceOhmsPerMeter(), 0.9, 230.0, 1));
		assertFalse(verifyResult.exceedsRecommendedLimit());
	}

	@Test
	void noVoltageDropCheck_resultFieldIsNull() {
		WireSizingInput input = new WireSizingInput(
				15.0, false, 30.0, 2, 1, InsulationType.THHN, ConductorMaterial.COPPER, 75, null);

		WireSizingResult result = calculator.calculate(input);

		// voltageDropCheck() == null in the input; calculate()'s only branch that calls
		// ConductorPropertiesResolver/VoltageDropCalculator is guarded behind that same null
		// check, so a null result here is structural proof neither was invoked.
		assertNull(result.voltageDropCheckResult());
	}

	@Test
	void parallelSets_singleConductorTooLargeForTable_splittingIntoTwoSucceeds() {
		// 1000A total load, 90C COPPER column, 30C ambient (factor 1.0), count=2 (no
		// adjustment). The largest published single conductor is 500 COPPER @ 90C = 595A --
		// nowhere near 1000A, so a single conductor (numberOfParallelSets=1) must throw.
		WireSizingInput singleConductorInput = new WireSizingInput(
				1000.0, false, 30.0, 2, 1, InsulationType.THHN, ConductorMaterial.COPPER, 90, null);
		assertThrows(CalculationException.class, () -> calculator.calculate(singleConductorInput));

		// Splitting across 2 parallel sets means each conductor only needs to satisfy 500A
		// (1000/2), not 1000A. Ascending COPPER 90C ampacities near that point: "325"=490A (too
		// small), "375"=530A (first size >= 500A) -- so "375" is recommended, not "500".
		WireSizingInput twoSetsInput = new WireSizingInput(
				1000.0, false, 30.0, 2, 2, InsulationType.THHN, ConductorMaterial.COPPER, 90, null);
		WireSizingResult result = calculator.calculate(twoSetsInput);

		assertEquals("375", result.recommendedSizeLabel());
		assertEquals(530.0, result.baseAmpacityAmps(), DELTA);
		assertEquals(530.0, result.deratedAmpacityAmps(), DELTA);
		assertEquals(2, result.numberOfParallelSets());
		assertEquals(500.0, result.requiredAmpacityPerSetAmps(), DELTA);
		assertEquals(1000.0, result.requiredAmpacityAmps(), DELTA);
		assertTrue(result.meetsTerminationRating());
	}

	/**
	 * Computes voltage drop percent for COPPER/PVC size "2.0" independently of both
	 * {@link WireSizingCalculator} and {@link ConductorPropertiesResolver} — resistance and
	 * reactance are the raw PEC Table 10.1.1.9 constants (3.1, 0.058 ohms per 305m) typed
	 * directly, not resolved through any calc-core class under test.
	 */
	private static double independentVoltageDropPercentForSize2_0(double currentAmps, double lengthMeters, double powerFactor, double systemVoltage) {
		double resistanceOhmsPerMeter = 3.1 / 305.0;
		double reactanceOhmsPerMeter = 0.058 / 305.0;
		double sinTheta = Math.sqrt(1 - powerFactor * powerFactor);
		double term = (resistanceOhmsPerMeter * powerFactor) + (reactanceOhmsPerMeter * sinTheta);
		double voltageDropVolts = 2 * lengthMeters * currentAmps * term;
		return (voltageDropVolts / systemVoltage) * 100.0;
	}

}
