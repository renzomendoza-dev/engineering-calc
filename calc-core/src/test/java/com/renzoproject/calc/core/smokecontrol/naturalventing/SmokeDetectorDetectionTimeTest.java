package com.renzoproject.calc.core.smokecontrol.naturalventing;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NFPA 204 (2021) Equation 9.2.5.4.3. The reference times were worked out with a separate
 * evaluation of the printed equation (tg, H, r, dT in s, m, m, C).
 */
class SmokeDetectorDetectionTimeTest {

	@Test
	void timeToRise_matchesTheEquationWorkedByHand() {
		// The Annex D smoke detector: tg 75 s, H 8.6 m, r 6.5 m, 10 C rise. The equation gives 54.6 s; Annex D prints 48 s from DETACT-QS.
		assertEquals(54.597, SmokeDetectorDetectionTime.timeToTemperatureRise(75.0, 8.6, 6.5, 10.0), 0.001);
		assertEquals(75.179, SmokeDetectorDetectionTime.timeToTemperatureRise(75.0, 8.6, 6.5, 20.0), 0.001);
		assertEquals(138.551, SmokeDetectorDetectionTime.timeToTemperatureRise(150.0, 9.1, 6.5, 20.0), 0.001);
		assertEquals(208.325, SmokeDetectorDetectionTime.timeToTemperatureRise(150.0, 9.1, 12.9, 20.0), 0.001);
		assertEquals(245.582, SmokeDetectorDetectionTime.timeToTemperatureRise(300.0, 8.6, 6.5, 20.0), 0.001);
	}

	@Test
	void theRiseAtThatTime_isTheRiseAskedFor() {
		double[][] cases = {{75, 8.6, 6.5, 10}, {150, 9.1, 12.9, 20}, {300, 12.0, 0.0, 35}, {45, 4.0, 3.0, 5}};
		for (double[] c : cases) {
			double t = SmokeDetectorDetectionTime.timeToTemperatureRise(c[0], c[1], c[2], c[3]);
			assertEquals(c[3], SmokeDetectorDetectionTime.temperatureRise(c[0], c[1], c[2], t), 1e-9);
		}
	}

	@Test
	void theRiseIsZeroBeforeThePlumeArrives_andGrowsAfterwards() {
		assertEquals(0.0, SmokeDetectorDetectionTime.temperatureRise(75.0, 8.6, 6.5, 1.0), 0.0);
		assertEquals(0.0, SmokeDetectorDetectionTime.temperatureRise(75.0, 8.6, 6.5, 0.0), 0.0);
		double earlier = SmokeDetectorDetectionTime.temperatureRise(150.0, 9.1, 6.5, 100.0);
		double later = SmokeDetectorDetectionTime.temperatureRise(150.0, 9.1, 6.5, 200.0);
		assertTrue(earlier > 0 && later > earlier);
	}

	@Test
	void aFartherDetector_aSlowerFire_andAHigherCeiling_allTakeLonger() {
		double base = SmokeDetectorDetectionTime.timeToTemperatureRise(150.0, 9.1, 6.5, 20.0);

		assertTrue(SmokeDetectorDetectionTime.timeToTemperatureRise(150.0, 9.1, 12.9, 20.0) > base);
		assertTrue(SmokeDetectorDetectionTime.timeToTemperatureRise(300.0, 9.1, 6.5, 20.0) > base);
		assertTrue(SmokeDetectorDetectionTime.timeToTemperatureRise(150.0, 12.0, 6.5, 20.0) > base);
		assertTrue(SmokeDetectorDetectionTime.timeToTemperatureRise(150.0, 9.1, 6.5, 30.0) > base);
	}

	@Test
	void minimumRise_isTwentyDegrees() {
		assertEquals(20.0, SmokeDetectorDetectionTime.MINIMUM_TEMPERATURE_RISE, 0.0);
	}

	@Test
	void badInputs_areRejected() {
		assertThrows(CalculationException.class, () -> SmokeDetectorDetectionTime.timeToTemperatureRise(0.0, 9.1, 6.5, 20.0));
		assertThrows(CalculationException.class, () -> SmokeDetectorDetectionTime.timeToTemperatureRise(150.0, 0.0, 6.5, 20.0));
		assertThrows(CalculationException.class, () -> SmokeDetectorDetectionTime.timeToTemperatureRise(150.0, 9.1, -1.0, 20.0));
		assertThrows(CalculationException.class, () -> SmokeDetectorDetectionTime.timeToTemperatureRise(150.0, 9.1, 6.5, 0.0));
		assertThrows(CalculationException.class, () -> SmokeDetectorDetectionTime.temperatureRise(150.0, 9.1, -1.0, 100.0));
	}

}
