package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PumpPowerCalculatorTest {

	private static final double DELTA = 1e-9;

	private final PumpMotorSizeResolver motorSizeResolver =
			new FakePumpMotorSizeResolver(List.of(7.5, 11.0, 15.0, 18.5, 22.0));
	private final PumpPowerCalculator calculator = new PumpPowerCalculator(motorSizeResolver);

	private static Quantity<com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate> flowRateM3s(double value) {
		return Quantities.getQuantity(value, PipeUnits.CUBIC_METRE_PER_SECOND);
	}

	private static Quantity<Length> lengthM(double value) {
		return Quantities.getQuantity(value, Units.METRE);
	}

	@Test
	void computesHydraulicAndShaftPower_andResolvesMotorSize() {
		// hydraulicPowerKw=12.2583125, shaftPowerKw=16.344416666666667 (precomputed).
		// Fake steps include ...15, 18.5... -> rounds up to 18.5.
		PumpPowerInput input = new PumpPowerInput(flowRateM3s(0.05), lengthM(25.0), 0.75, 1000.0);

		PumpPowerResult result = calculator.calculate(input);

		assertEquals(12.2583125, result.hydraulicPowerKw(), DELTA);
		assertEquals(16.344416666666667, result.shaftPowerKw(), DELTA);
		assertEquals("18.5 kW", result.recommendedMotorSizeKw());
	}

	@Test
	void motorSizeExceedsLargestFakeStep_throwsRatherThanClamping() {
		PumpPowerInput input = new PumpPowerInput(flowRateM3s(5.0), lengthM(100.0), 0.75, 1000.0);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void nonPositiveFlowRate_throws() {
		assertThrows(CalculationException.class, () -> new PumpPowerInput(flowRateM3s(0.0), lengthM(25.0), 0.75, 1000.0));
	}

	@Test
	void nonPositiveTotalDynamicHead_throws() {
		// Unlike PumpTDHCalculator, a non-positive head IS invalid for power/motor sizing.
		assertThrows(CalculationException.class, () -> new PumpPowerInput(flowRateM3s(0.05), lengthM(0.0), 0.75, 1000.0));
	}

	@Test
	void pumpEfficiencyOutOfRange_throws() {
		assertThrows(CalculationException.class, () -> new PumpPowerInput(flowRateM3s(0.05), lengthM(25.0), 0.0, 1000.0));
		assertThrows(CalculationException.class, () -> new PumpPowerInput(flowRateM3s(0.05), lengthM(25.0), 1.1, 1000.0));
	}

	@Test
	void nonPositiveFluidDensity_throws() {
		assertThrows(CalculationException.class, () -> new PumpPowerInput(flowRateM3s(0.05), lengthM(25.0), 0.75, 0.0));
	}

}
