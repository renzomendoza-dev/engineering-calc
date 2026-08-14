package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.quantity.Speed;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipeVelocityCalculatorTest {

	private static final double DELTA = 1e-9;
	private static final Unit<Length> MILLIMETRE = MetricPrefix.MILLI(Units.METRE);

	// Shared fake catalog for design-mode tests: "80" is too small for the scenario C target,
	// "100" is the smallest sufficient size, and both cap what's available for scenario D
	// (no size large enough).
	private final FakePipeDimensionResolver resolver = new FakePipeDimensionResolver(List.of(
			FakePipeDimensionResolver.FakeSize.of("TESTMAT", "SCH1", "50", "50mm (test)", 50.0),
			FakePipeDimensionResolver.FakeSize.of("TESTMAT", "SCH1", "80", "80mm (test)", 80.0),
			FakePipeDimensionResolver.FakeSize.of("TESTMAT", "SCH1", "100", "100mm (test)", 100.0)));

	private final PipeVelocityCalculator calculator = new PipeVelocityCalculator(resolver);

	private static Quantity<VolumetricFlowRate> flowRateM3s(double value) {
		return Quantities.getQuantity(value, PipeUnits.CUBIC_METRE_PER_SECOND);
	}

	private static Quantity<Speed> speedMs(double value) {
		return Quantities.getQuantity(value, Units.METRE_PER_SECOND);
	}

	private static Quantity<Length> lengthMm(double value) {
		return Quantities.getQuantity(value, MILLIMETRE);
	}

	@Test
	void velocityFromDiameter_rawDiameter_matchesHandVerifiedValue() {
		// D=50mm, Q=0.002 m3/s -> V = Q / ((pi/4)*D^2) = 1.0185916357881302 m/s (precomputed).
		PipeVelocityInput input = new PipeVelocityInput(
				PipeSizingMode.VELOCITY_FROM_DIAMETER, flowRateM3s(0.002),
				new RawDiameter(lengthMm(50.0)), null, null, null);

		PipeSizingResult result = calculator.calculate(input);

		assertInstanceOf(VelocityResult.class, result);
		double velocityMs = ((VelocityResult) result).velocity().to(Units.METRE_PER_SECOND).getValue().doubleValue();
		assertEquals(1.0185916357881302, velocityMs, DELTA);
	}

	@Test
	void velocityFromDiameter_nominalSize_resolverRoundTripMatchesRawDiameterResult() {
		// Same D=50mm (via the fake catalog's "50" entry) and Q=0.002 m3/s as the raw-diameter
		// case -> same expected velocity, proving the resolver round-trip doesn't change the math.
		PipeVelocityInput input = new PipeVelocityInput(
				PipeSizingMode.VELOCITY_FROM_DIAMETER, flowRateM3s(0.002),
				new NominalSize("TESTMAT", "SCH1", "50"), null, null, null);

		PipeSizingResult result = calculator.calculate(input);

		assertInstanceOf(VelocityResult.class, result);
		double velocityMs = ((VelocityResult) result).velocity().to(Units.METRE_PER_SECOND).getValue().doubleValue();
		assertEquals(1.0185916357881302, velocityMs, DELTA);
	}

	@Test
	void velocityFromDiameter_unknownNominalSize_throws() {
		PipeVelocityInput input = new PipeVelocityInput(
				PipeSizingMode.VELOCITY_FROM_DIAMETER, flowRateM3s(0.002),
				new NominalSize("TESTMAT", "SCH1", "999"), null, null, null);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void diameterFromVelocity_producesResultWithActualVelocityAtOrBelowTarget() {
		// Q=0.01 m3/s, target=1.5 m/s -> Dmin = sqrt(4Q/(pi*V)) = 0.09213177319235613 m
		// (92.13mm), so the fake catalog's "80" (too small) is rejected and "100" is chosen.
		// Actual velocity at 100mm = 1.2732395447351625 m/s (precomputed), which is <= 1.5.
		PipeVelocityInput input = new PipeVelocityInput(
				PipeSizingMode.DIAMETER_FROM_VELOCITY, flowRateM3s(0.01),
				null, speedMs(1.5), "TESTMAT", "SCH1");

		PipeSizingResult result = calculator.calculate(input);

		assertInstanceOf(DiameterSizingResult.class, result);
		DiameterSizingResult sizing = (DiameterSizingResult) result;
		assertEquals(0.09213177319235613, sizing.calculatedMinDiameter().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals("100mm (test)", sizing.nominalPipeSize());
		assertEquals(100.0, sizing.actualInternalDiameter().to(MILLIMETRE).getValue().doubleValue(), DELTA);
		double actualVelocityMs = sizing.actualVelocity().to(Units.METRE_PER_SECOND).getValue().doubleValue();
		assertEquals(1.2732395447351625, actualVelocityMs, DELTA);
		assertTrue(actualVelocityMs <= 1.5);
	}

	@Test
	void diameterFromVelocity_noStandardSizeLargeEnough_throwsRatherThanClamping() {
		// Q=0.01 m3/s, target=0.5 m/s -> Dmin = 0.15957691216057307 m (159.58mm), which exceeds
		// the fake catalog's largest size (100mm) -> resolver must throw, not return "100".
		PipeVelocityInput input = new PipeVelocityInput(
				PipeSizingMode.DIAMETER_FROM_VELOCITY, flowRateM3s(0.01),
				null, speedMs(0.5), "TESTMAT", "SCH1");

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void flowRateNotPositive_throws() {
		assertThrows(CalculationException.class, () -> new PipeVelocityInput(
				PipeSizingMode.VELOCITY_FROM_DIAMETER, flowRateM3s(0.0),
				new RawDiameter(lengthMm(50.0)), null, null, null));
	}

	@Test
	void velocityFromDiameter_missingDiameterSpec_throws() {
		assertThrows(CalculationException.class, () -> new PipeVelocityInput(
				PipeSizingMode.VELOCITY_FROM_DIAMETER, flowRateM3s(0.002),
				null, null, null, null));
	}

	@Test
	void velocityFromDiameter_rawDiameterNotPositive_throws() {
		assertThrows(CalculationException.class, () -> new PipeVelocityInput(
				PipeSizingMode.VELOCITY_FROM_DIAMETER, flowRateM3s(0.002),
				new RawDiameter(lengthMm(0.0)), null, null, null));
	}

	@Test
	void diameterFromVelocity_missingTargetVelocity_throws() {
		assertThrows(CalculationException.class, () -> new PipeVelocityInput(
				PipeSizingMode.DIAMETER_FROM_VELOCITY, flowRateM3s(0.01),
				null, null, "TESTMAT", "SCH1"));
	}

	@Test
	void diameterFromVelocity_targetVelocityNotPositive_throws() {
		assertThrows(CalculationException.class, () -> new PipeVelocityInput(
				PipeSizingMode.DIAMETER_FROM_VELOCITY, flowRateM3s(0.01),
				null, speedMs(0.0), "TESTMAT", "SCH1"));
	}

	@Test
	void diameterFromVelocity_missingPipeMaterial_throws() {
		assertThrows(CalculationException.class, () -> new PipeVelocityInput(
				PipeSizingMode.DIAMETER_FROM_VELOCITY, flowRateM3s(0.01),
				null, speedMs(1.5), null, "SCH1"));
	}

	@Test
	void diameterFromVelocity_blankPipeMaterial_throws() {
		assertThrows(CalculationException.class, () -> new PipeVelocityInput(
				PipeSizingMode.DIAMETER_FROM_VELOCITY, flowRateM3s(0.01),
				null, speedMs(1.5), "  ", "SCH1"));
	}

}
