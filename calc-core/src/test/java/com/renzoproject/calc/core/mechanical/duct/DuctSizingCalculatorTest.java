package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.FluidProperties;
import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuctSizingCalculatorTest {

	private static final double DELTA = 1e-6;
	private static final double FORWARD_CHECK_RELATIVE_TOLERANCE = 1e-3;

	private final AirPropertiesResolver airPropertiesResolver = new FakeAirPropertiesResolver(new FluidProperties(1.2, 1.8e-5));
	private final DuctRoughnessResolver roughnessResolver = new FakeDuctRoughnessResolver(Map.of("TESTMAT", 0.09));

	private final DuctSizingCalculator calculator = new DuctSizingCalculator(airPropertiesResolver, roughnessResolver);

	private static Quantity<VolumetricFlowRate> flowRate(double m3s) {
		return Quantities.getQuantity(m3s, PipeUnits.CUBIC_METRE_PER_SECOND);
	}

	private static Quantity<Temperature> anyTemperature() {
		return Quantities.getQuantity(20.0, Units.CELSIUS);
	}

	private static Quantity<Length> noAltitude() {
		return Quantities.getQuantity(0.0, Units.METRE);
	}

	private static Quantity<Length> metres(double value) {
		return Quantities.getQuantity(value, Units.METRE);
	}

	private static Quantity<Pressure> paPerMetre(double value) {
		return Quantities.getQuantity(value, Units.PASCAL);
	}

	private static Quantity<Speed> metresPerSecond(double value) {
		return Quantities.getQuantity(value, Units.METRE_PER_SECOND);
	}

	private static DuctSizingInput roundVelocityInput(double airFlowM3s, double maxVelocityMs) {
		return new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(airFlowM3s), anyTemperature(), noAltitude(),
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.ROUND,
				null, null, null, metresPerSecond(maxVelocityMs));
	}

	private static DuctSizingInput roundEqualFrictionInput(double airFlowM3s, double targetRatePaPerM, FrictionFactorMethod method) {
		return new DuctSizingInput(
				DuctSizingMethod.EQUAL_FRICTION, flowRate(airFlowM3s), anyTemperature(), noAltitude(),
				"TESTMAT", method, DuctShape.ROUND,
				null, null, paPerMetre(targetRatePaPerM), null);
	}

	@Test
	void round_velocity_matchesClosedFormDiameter() {
		double airFlowM3s = 1.0;
		double maxVelocityMs = 5.0;
		DuctSizingResult result = calculator.calculate(roundVelocityInput(airFlowM3s, maxVelocityMs));

		double expectedDiameterM = Math.sqrt((4.0 * airFlowM3s) / (Math.PI * maxVelocityMs));
		assertEquals(expectedDiameterM, result.equivalentDiameter().to(Units.METRE).getValue().doubleValue(), 1e-9);
		assertEquals(maxVelocityMs, result.actualVelocity().to(Units.METRE_PER_SECOND).getValue().doubleValue(), 1e-6);
		assertNull(result.ductWidth());
		assertNull(result.ductHeight());
		assertTrue(result.reynoldsNumber() > 0);
	}

	@Test
	void round_equalFriction_swameeJain_convergesToTargetFrictionRate() {
		double targetRatePaPerM = 1.0;
		DuctSizingResult result = calculator.calculate(roundEqualFrictionInput(1.0, targetRatePaPerM, FrictionFactorMethod.SWAMEE_JAIN));

		double actualRate = result.actualFrictionRatePerMeter().to(Units.PASCAL).getValue().doubleValue();
		double relativeDiff = Math.abs(actualRate - targetRatePaPerM) / targetRatePaPerM;
		assertTrue(relativeDiff < FORWARD_CHECK_RELATIVE_TOLERANCE,
				"Expected actualFrictionRatePerMeter close to target " + targetRatePaPerM + ", was " + actualRate);
	}

	@Test
	void round_equalFriction_colebrookWhite_convergesToTargetFrictionRate() {
		double targetRatePaPerM = 1.0;
		DuctSizingResult result = calculator.calculate(roundEqualFrictionInput(1.0, targetRatePaPerM, FrictionFactorMethod.COLEBROOK_WHITE));

		double actualRate = result.actualFrictionRatePerMeter().to(Units.PASCAL).getValue().doubleValue();
		double relativeDiff = Math.abs(actualRate - targetRatePaPerM) / targetRatePaPerM;
		assertTrue(relativeDiff < FORWARD_CHECK_RELATIVE_TOLERANCE,
				"Expected actualFrictionRatePerMeter close to target " + targetRatePaPerM + ", was " + actualRate);
	}

	@Test
	void rectangular_velocity_fixedHeight_equivalentDiameterMatchesRoundTarget() {
		double airFlowM3s = 1.0;
		double maxVelocityMs = 5.0;
		double targetDiameterM = Math.sqrt((4.0 * airFlowM3s) / (Math.PI * maxVelocityMs));

		DuctSizingInput input = new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(airFlowM3s), anyTemperature(), noAltitude(),
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.RECTANGULAR,
				FixedDimensionType.HEIGHT, metres(0.3), null, metresPerSecond(maxVelocityMs));

		DuctSizingResult result = calculator.calculate(input);

		double actualEquivalentDiameterM = result.equivalentDiameter().to(Units.METRE).getValue().doubleValue();
		double relativeDiff = Math.abs(actualEquivalentDiameterM - targetDiameterM) / targetDiameterM;
		assertTrue(relativeDiff < FORWARD_CHECK_RELATIVE_TOLERANCE,
				"Expected equivalentDiameter close to round-equivalent target " + targetDiameterM + ", was " + actualEquivalentDiameterM);

		assertNotNull(result.ductWidth());
		assertNotNull(result.ductHeight());
		assertEquals(0.3, result.ductHeight().to(Units.METRE).getValue().doubleValue(), DELTA);

		double widthM = result.ductWidth().to(Units.METRE).getValue().doubleValue();
		double heightM = result.ductHeight().to(Units.METRE).getValue().doubleValue();
		double expectedVelocity = airFlowM3s / (widthM * heightM);
		assertEquals(expectedVelocity, result.actualVelocity().to(Units.METRE_PER_SECOND).getValue().doubleValue(), 1e-6);
	}

	@Test
	void rectangular_equalFriction_fixedWidth_forwardCheckMatchesTarget() {
		double targetRatePaPerM = 1.0;
		DuctSizingInput input = new DuctSizingInput(
				DuctSizingMethod.EQUAL_FRICTION, flowRate(1.0), anyTemperature(), noAltitude(),
				"TESTMAT", FrictionFactorMethod.COLEBROOK_WHITE, DuctShape.RECTANGULAR,
				FixedDimensionType.WIDTH, metres(0.4), paPerMetre(targetRatePaPerM), null);

		DuctSizingResult result = calculator.calculate(input);

		double actualRate = result.actualFrictionRatePerMeter().to(Units.PASCAL).getValue().doubleValue();
		double relativeDiff = Math.abs(actualRate - targetRatePaPerM) / targetRatePaPerM;
		assertTrue(relativeDiff < FORWARD_CHECK_RELATIVE_TOLERANCE,
				"Expected actualFrictionRatePerMeter close to target " + targetRatePaPerM + ", was " + actualRate);
		assertEquals(0.4, result.ductWidth().to(Units.METRE).getValue().doubleValue(), DELTA);
	}

	@Test
	void nonPositiveAirFlow_throws() {
		assertThrows(CalculationException.class, () -> roundVelocityInput(0.0, 5.0));
		assertThrows(CalculationException.class, () -> roundVelocityInput(-1.0, 5.0));
	}

	@Test
	void rectangular_missingFixedDimensionType_throws() {
		assertThrows(CalculationException.class, () -> new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(1.0), anyTemperature(), noAltitude(),
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.RECTANGULAR,
				null, metres(0.3), null, metresPerSecond(5.0)));
	}

	@Test
	void rectangular_missingFixedDimensionValue_throws() {
		assertThrows(CalculationException.class, () -> new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(1.0), anyTemperature(), noAltitude(),
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.RECTANGULAR,
				FixedDimensionType.HEIGHT, null, null, metresPerSecond(5.0)));
	}

	@Test
	void rectangular_nonPositiveFixedDimensionValue_throws() {
		assertThrows(CalculationException.class, () -> new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(1.0), anyTemperature(), noAltitude(),
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.RECTANGULAR,
				FixedDimensionType.HEIGHT, metres(0.0), null, metresPerSecond(5.0)));
	}

	@Test
	void equalFriction_missingTargetFrictionRate_throws() {
		assertThrows(CalculationException.class, () -> new DuctSizingInput(
				DuctSizingMethod.EQUAL_FRICTION, flowRate(1.0), anyTemperature(), noAltitude(),
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.ROUND,
				null, null, null, null));
	}

	@Test
	void equalFriction_nonPositiveTargetFrictionRate_throws() {
		assertThrows(CalculationException.class, () -> roundEqualFrictionInput(1.0, 0.0, FrictionFactorMethod.SWAMEE_JAIN));
		assertThrows(CalculationException.class, () -> roundEqualFrictionInput(1.0, -1.0, FrictionFactorMethod.SWAMEE_JAIN));
	}

	@Test
	void velocity_missingMaxVelocity_throws() {
		assertThrows(CalculationException.class, () -> new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(1.0), anyTemperature(), noAltitude(),
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.ROUND,
				null, null, null, null));
	}

	@Test
	void velocity_nonPositiveMaxVelocity_throws() {
		assertThrows(CalculationException.class, () -> roundVelocityInput(1.0, 0.0));
		assertThrows(CalculationException.class, () -> roundVelocityInput(1.0, -5.0));
	}

	@Test
	void missingCoreRequiredFields_throw() {
		assertThrows(CalculationException.class, () -> new DuctSizingInput(
				null, flowRate(1.0), anyTemperature(), noAltitude(),
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.ROUND, null, null, null, metresPerSecond(5.0)));
		assertThrows(CalculationException.class, () -> new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(1.0), null, noAltitude(),
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.ROUND, null, null, null, metresPerSecond(5.0)));
		assertThrows(CalculationException.class, () -> new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(1.0), anyTemperature(), null,
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.ROUND, null, null, null, metresPerSecond(5.0)));
		assertThrows(CalculationException.class, () -> new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(1.0), anyTemperature(), noAltitude(),
				null, FrictionFactorMethod.SWAMEE_JAIN, DuctShape.ROUND, null, null, null, metresPerSecond(5.0)));
		assertThrows(CalculationException.class, () -> new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(1.0), anyTemperature(), noAltitude(),
				"TESTMAT", null, DuctShape.ROUND, null, null, null, metresPerSecond(5.0)));
		assertThrows(CalculationException.class, () -> new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(1.0), anyTemperature(), noAltitude(),
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, null, null, null, null, metresPerSecond(5.0)));
	}

	@Test
	void resolverException_unknownDuctMaterial_propagates() {
		DuctSizingInput input = new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(1.0), anyTemperature(), noAltitude(),
				"NOT_A_REAL_MATERIAL", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.ROUND,
				null, null, null, metresPerSecond(5.0));

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void roundEqualFriction_absurdlyLowTargetFrictionRate_throwsRatherThanExtrapolating() {
		// A near-zero target implies an enormous diameter, likely outside the bisection bracket
		// (10x the velocity-based seed guess) -- must throw, not silently return a bad answer.
		DuctSizingInput input = roundEqualFrictionInput(1.0, 1e-12, FrictionFactorMethod.SWAMEE_JAIN);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void rectangular_freeDimensionWouldExceedSaneUpperBound_throws() {
		// A large round-equivalent target combined with a tiny fixed dimension forces the free
		// dimension to be non-physically large to reach that target.
		DuctSizingInput input = new DuctSizingInput(
				DuctSizingMethod.VELOCITY, flowRate(100.0), anyTemperature(), noAltitude(),
				"TESTMAT", FrictionFactorMethod.SWAMEE_JAIN, DuctShape.RECTANGULAR,
				FixedDimensionType.HEIGHT, metres(0.01), null, metresPerSecond(0.5));

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

}
