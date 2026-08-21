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
