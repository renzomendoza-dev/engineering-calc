package com.renzoproject.calc.core.electrical.voltagedrop;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoltageDropCalculatorTest {

	private static final double DELTA = 1e-6;

	private final VoltageDropCalculator calculator = new VoltageDropCalculator();

	@Test
	void dcCircuit_matchesHandCalculatedValue() {
		// r = 0.0005 ohm/m, Vdrop = 2 * 50m * 100A * 0.0005 = 5.0V
		VoltageDropInput input = new VoltageDropInput(
				CircuitType.DC, 100.0, 50.0, 0.0005, 0.0, 1.0, 48.0, 1);

		VoltageDropResult result = calculator.calculate(input);

		assertEquals(5.0, result.voltageDropVolts(), DELTA);
		assertEquals(10.416666666666666, result.voltageDropPercent(), DELTA);
		assertEquals(43.0, result.receivingEndVoltage(), DELTA);
		assertTrue(result.exceedsRecommendedLimit());
	}

	@Test
	void singlePhaseAc_realisticPowerFactor() {
		// sinTheta = sqrt(1 - 0.9^2) = sqrt(0.19)
		// term = (0.002 * 0.9) + (0.0008 * sqrt(0.19)) = 0.0021487119154832...
		// Vdrop = 2 * 75m * 20A * term
		VoltageDropInput input = new VoltageDropInput(
				CircuitType.SINGLE_PHASE_AC, 20.0, 75.0, 0.002, 0.0008, 0.9, 230.0, 1);

		VoltageDropResult result = calculator.calculate(input);

		double sinTheta = Math.sqrt(1 - 0.9 * 0.9);
		double expectedTerm = (0.002 * 0.9) + (0.0008 * sinTheta);
		double expectedVdrop = 2 * 75.0 * 20.0 * expectedTerm;

		assertEquals(expectedVdrop, result.voltageDropVolts(), DELTA);
		assertEquals(expectedVdrop / 230.0 * 100.0, result.voltageDropPercent(), DELTA);
		assertEquals(230.0 - expectedVdrop, result.receivingEndVoltage(), DELTA);
		assertFalse(result.exceedsRecommendedLimit());
	}

	@Test
	void threePhaseAc_realisticPowerFactor() {
		VoltageDropInput input = new VoltageDropInput(
				CircuitType.THREE_PHASE_AC, 40.0, 120.0, 0.0015, 0.0006, 0.85, 400.0, 1);

		VoltageDropResult result = calculator.calculate(input);

		double sinTheta = Math.sqrt(1 - 0.85 * 0.85);
		double expectedTerm = (0.0015 * 0.85) + (0.0006 * sinTheta);
		double expectedVdrop = Math.sqrt(3) * 120.0 * 40.0 * expectedTerm;

		assertEquals(expectedVdrop, result.voltageDropVolts(), DELTA);
		assertEquals(expectedVdrop / 400.0 * 100.0, result.voltageDropPercent(), DELTA);
		assertEquals(400.0 - expectedVdrop, result.receivingEndVoltage(), DELTA);
		assertTrue(result.exceedsRecommendedLimit());
	}

	@Test
	void parallelSets_divideResistanceAndReactanceCorrectly() {
		VoltageDropInput singleSet = new VoltageDropInput(
				CircuitType.SINGLE_PHASE_AC, 20.0, 75.0, 0.002, 0.0008, 0.9, 230.0, 1);
		// Same conductor run in 4 parallel sets: r/x scaled by 4 so the per-set
		// value after division by parallelSetsPerPhase matches singleSet exactly.
		VoltageDropInput fourSets = new VoltageDropInput(
				CircuitType.SINGLE_PHASE_AC, 20.0, 75.0, 0.008, 0.0032, 0.9, 230.0, 4);

		VoltageDropResult singleSetResult = calculator.calculate(singleSet);
		VoltageDropResult fourSetsResult = calculator.calculate(fourSets);

		assertEquals(singleSetResult.voltageDropVolts(), fourSetsResult.voltageDropVolts(), 1e-9);
		assertEquals(singleSetResult.voltageDropPercent(), fourSetsResult.voltageDropPercent(), 1e-9);
	}

	@Test
	void exceedsRecommendedLimit_whenVoltageDropAboveThreshold() {
		// pf = 1.0 => sinTheta = 0, term = r = 0.001
		// Vdrop = 2 * 150m * 50A * 0.001 = 15.0V, 15/120*100 = 12.5%
		VoltageDropInput input = new VoltageDropInput(
				CircuitType.SINGLE_PHASE_AC, 50.0, 150.0, 0.001, 0.0005, 1.0, 120.0, 1);

		VoltageDropResult result = calculator.calculate(input);

		assertEquals(15.0, result.voltageDropVolts(), DELTA);
		assertEquals(12.5, result.voltageDropPercent(), DELTA);
		assertEquals(105.0, result.receivingEndVoltage(), DELTA);
		assertTrue(result.exceedsRecommendedLimit());
		assertTrue(result.voltageDropPercent() > VoltageDropCalculator.RECOMMENDED_MAX_PERCENT);
	}

	@Test
	void rejectsNegativeCurrent() {
		assertThrows(CalculationException.class, () -> new VoltageDropInput(
				CircuitType.SINGLE_PHASE_AC, -5.0, 50.0, 0.001, 0.0004, 0.9, 230.0, 1));
	}

	@Test
	void rejectsInvalidPowerFactorForAc() {
		assertThrows(CalculationException.class, () -> new VoltageDropInput(
				CircuitType.SINGLE_PHASE_AC, 20.0, 50.0, 0.001, 0.0004, 1.5, 230.0, 1));
	}

	@Test
	void rejectsNonZeroReactanceForDc() {
		assertThrows(CalculationException.class, () -> new VoltageDropInput(
				CircuitType.DC, 20.0, 50.0, 0.001, 0.0004, 1.0, 48.0, 1));
	}

}
