package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipePressureLossCalculatorTest {

	private static final double DELTA = 1e-9;
	private static final Unit<Length> MILLIMETRE = MetricPrefix.MILLI(Units.METRE);

	private final FakePipeDimensionResolver dimensionResolver = new FakePipeDimensionResolver(List.of(
			FakePipeDimensionResolver.FakeSize.of("TESTMAT", "SCH1", "100", "100mm (test)", 100.0),
			FakePipeDimensionResolver.FakeSize.of("TESTMAT", "SCH1", "50", "50mm (test)", 50.0)));
	private final FakePipeRoughnessResolver roughnessResolver = new FakePipeRoughnessResolver(Map.of("TESTMAT", 0.1));

	private static Quantity<Temperature> anyTemperature() {
		return Quantities.getQuantity(20.0, Units.CELSIUS);
	}

	private static Quantity<Length> lengthM(double value) {
		return Quantities.getQuantity(value, Units.METRE);
	}

	private static Quantity<Length> lengthMm(double value) {
		return Quantities.getQuantity(value, MILLIMETRE);
	}

	private static Quantity<VolumetricFlowRate> flowRateM3s(double value) {
		return Quantities.getQuantity(value, PipeUnits.CUBIC_METRE_PER_SECOND);
	}

	@Test
	void turbulentFlow_swameeJain_matchesHandVerifiedValues() {
		// D=100mm (via NominalSize -- roughness lookup needs a material), Q=0.02 m3/s,
		// density=1000, viscosity=0.001, roughness=0.1mm, L=50m.
		// V=2.546479089470325, Re=254647.90894703253 (turbulent), relRoughness=0.001.
		// Swamee-Jain f=0.02090989762777186 (precomputed independently via the same formula).
		PipePressureLossCalculator calculator = new PipePressureLossCalculator(
				dimensionResolver, new FakeFluidPropertiesResolver(1000.0, 0.001), roughnessResolver);
		PipePressureLossInput input = new PipePressureLossInput(
				"TESTFLUID", anyTemperature(), flowRateM3s(0.02),
				new NominalSize("TESTMAT", "SCH1", "100"),
				lengthM(50.0), FrictionFactorMethod.SWAMEE_JAIN);

		PipePressureLossResult result = calculator.calculate(input);

		assertEquals(2.546479089470325, result.velocity().to(Units.METRE_PER_SECOND).getValue().doubleValue(), DELTA);
		assertEquals(254647.90894703253, result.reynoldsNumber(), 1e-6);
		assertEquals(FlowRegime.TURBULENT, result.flowRegime());
		assertFalse(result.transitionalRegimeWarning());
		assertEquals(0.02090989762777186, result.frictionFactor(), 1e-9);
		assertEquals(3.4566186454880397, result.headLoss().to(Units.METRE).getValue().doubleValue(), 1e-6);
		assertEquals(33897.84923977528, result.pressureLoss().to(Units.PASCAL).getValue().doubleValue(), 1e-3);
	}

	@Test
	void turbulentFlow_colebrookWhite_convergesCloseToSwameeJain() {
		// Same scenario as the Swamee-Jain test. Colebrook f=0.0207600530848401 (precomputed via
		// the standard x=1/sqrt(f) fixed-point iteration, converging in 4 iterations).
		PipePressureLossCalculator calculator = new PipePressureLossCalculator(
				dimensionResolver, new FakeFluidPropertiesResolver(1000.0, 0.001), roughnessResolver);
		PipePressureLossInput input = new PipePressureLossInput(
				"TESTFLUID", anyTemperature(), flowRateM3s(0.02),
				new NominalSize("TESTMAT", "SCH1", "100"),
				lengthM(50.0), FrictionFactorMethod.COLEBROOK_WHITE);

		PipePressureLossResult result = calculator.calculate(input);

		assertEquals(0.0207600530848401, result.frictionFactor(), 1e-9);
		// Sanity check: Colebrook-White and Swamee-Jain should agree closely for typical
		// turbulent pipe flow -- confirm the relative difference is small (~0.7% here).
		double swameeJain = 0.02090989762777186;
		double relativeDiff = Math.abs(result.frictionFactor() - swameeJain) / swameeJain;
		assertTrue(relativeDiff < 0.02, "Colebrook-White and Swamee-Jain should agree within 2% for this case, was " + relativeDiff);
	}

	@Test
	void laminarFlow_usesSixtyFourOverReynoldsRegardlessOfMethod() {
		// D=50mm (via RawDiameter -- laminar flow needs no roughness, so this works fine even
		// though RawDiameter carries no material), Q=0.0001 m3/s, density=1000, viscosity=0.05
		// (viscous fluid) -> Re=50.93 (laminar). f=64/Re=1.2566370614359172, regardless of method.
		PipePressureLossCalculator calculator = new PipePressureLossCalculator(
				dimensionResolver, new FakeFluidPropertiesResolver(1000.0, 0.05), roughnessResolver);

		for (FrictionFactorMethod method : FrictionFactorMethod.values()) {
			PipePressureLossInput input = new PipePressureLossInput(
					"TESTFLUID", anyTemperature(), flowRateM3s(0.0001),
					new RawDiameter(lengthMm(50.0)),
					lengthM(50.0), method);

			PipePressureLossResult result = calculator.calculate(input);

			assertEquals(50.929581789406505, result.reynoldsNumber(), 1e-6);
			assertEquals(FlowRegime.LAMINAR, result.flowRegime());
			assertFalse(result.transitionalRegimeWarning());
			assertEquals(1.2566370614359172, result.frictionFactor(), 1e-9);
			assertEquals(0.16618790486669846, result.headLoss().to(Units.METRE).getValue().doubleValue(), 1e-6);
			assertEquals(1629.746617261008, result.pressureLoss().to(Units.PASCAL).getValue().doubleValue(), 1e-3);
		}
	}

	@Test
	void transitionalFlow_warningTrueAndCalculationCompletesUsingTurbulentCorrelation() {
		// D=100mm, Q chosen so Re=3000 exactly (2300 < 3000 < 4000 -> TRANSITIONAL). Needs a
		// NominalSize, same reasoning as the turbulent tests -- the turbulent correlation still
		// applies here per the documented "proceed anyway, just flag it" practice.
		PipePressureLossCalculator calculator = new PipePressureLossCalculator(
				dimensionResolver, new FakeFluidPropertiesResolver(1000.0, 0.001), roughnessResolver);
		PipePressureLossInput input = new PipePressureLossInput(
				"TESTFLUID", anyTemperature(), flowRateM3s(0.0002356194490192345),
				new NominalSize("TESTMAT", "SCH1", "100"),
				lengthM(50.0), FrictionFactorMethod.SWAMEE_JAIN);

		PipePressureLossResult result = calculator.calculate(input);

		assertEquals(3000.0, result.reynoldsNumber(), 1e-6);
		assertEquals(FlowRegime.TRANSITIONAL, result.flowRegime());
		assertTrue(result.transitionalRegimeWarning());
		assertEquals(0.04550962445356021, result.frictionFactor(), 1e-9);
	}

	@Test
	void nominalSizeAndRawDiameter_produceIdenticalResultsInLaminarFlow() {
		// Both DiameterSpec variants work when flow is laminar (no roughness needed) -- confirm
		// NominalSize("TESTMAT","SCH1","50") [resolves to 50mm] and RawDiameter(50mm) agree.
		PipePressureLossCalculator calculator = new PipePressureLossCalculator(
				dimensionResolver, new FakeFluidPropertiesResolver(1000.0, 0.05), roughnessResolver);

		PipePressureLossInput viaNominal = new PipePressureLossInput(
				"TESTFLUID", anyTemperature(), flowRateM3s(0.0001),
				new NominalSize("TESTMAT", "SCH1", "50"),
				lengthM(50.0), FrictionFactorMethod.SWAMEE_JAIN);
		PipePressureLossInput viaRaw = new PipePressureLossInput(
				"TESTFLUID", anyTemperature(), flowRateM3s(0.0001),
				new RawDiameter(lengthMm(50.0)),
				lengthM(50.0), FrictionFactorMethod.SWAMEE_JAIN);

		PipePressureLossResult resultNominal = calculator.calculate(viaNominal);
		PipePressureLossResult resultRaw = calculator.calculate(viaRaw);

		assertEquals(resultRaw.reynoldsNumber(), resultNominal.reynoldsNumber(), DELTA);
		assertEquals(resultRaw.frictionFactor(), resultNominal.frictionFactor(), DELTA);
		assertEquals(
				resultRaw.headLoss().to(Units.METRE).getValue().doubleValue(),
				resultNominal.headLoss().to(Units.METRE).getValue().doubleValue(), DELTA);
	}

	@Test
	void rawDiameterWithTurbulentFlow_throwsBecauseNoMaterialForRoughness() {
		// Same D/Q/fluid as the turbulent Swamee-Jain test (which succeeds via NominalSize) --
		// but RawDiameter carries no material, so the roughness lookup can't happen.
		PipePressureLossCalculator calculator = new PipePressureLossCalculator(
				dimensionResolver, new FakeFluidPropertiesResolver(1000.0, 0.001), roughnessResolver);
		PipePressureLossInput input = new PipePressureLossInput(
				"TESTFLUID", anyTemperature(), flowRateM3s(0.02),
				new RawDiameter(lengthMm(100.0)),
				lengthM(50.0), FrictionFactorMethod.SWAMEE_JAIN);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void resolvedDiameterNotPositive_throws() {
		FakePipeDimensionResolver zeroDiameterResolver = new FakePipeDimensionResolver(List.of(
				FakePipeDimensionResolver.FakeSize.of("TESTMAT", "SCH1", "0", "zero (test)", 0.0)));
		PipePressureLossCalculator calculator = new PipePressureLossCalculator(
				zeroDiameterResolver, new FakeFluidPropertiesResolver(1000.0, 0.001), roughnessResolver);
		PipePressureLossInput input = new PipePressureLossInput(
				"TESTFLUID", anyTemperature(), flowRateM3s(0.02),
				new NominalSize("TESTMAT", "SCH1", "0"),
				lengthM(50.0), FrictionFactorMethod.SWAMEE_JAIN);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void nonPositiveFlowRate_throws() {
		assertThrows(CalculationException.class, () -> new PipePressureLossInput(
				"TESTFLUID", anyTemperature(), flowRateM3s(0.0),
				new RawDiameter(lengthMm(100.0)),
				lengthM(50.0), FrictionFactorMethod.SWAMEE_JAIN));
	}

	@Test
	void nonPositivePipeLength_throws() {
		assertThrows(CalculationException.class, () -> new PipePressureLossInput(
				"TESTFLUID", anyTemperature(), flowRateM3s(0.02),
				new RawDiameter(lengthMm(100.0)),
				lengthM(0.0), FrictionFactorMethod.SWAMEE_JAIN));
	}

	@Test
	void rawDiameterNotPositive_throws() {
		assertThrows(CalculationException.class, () -> new PipePressureLossInput(
				"TESTFLUID", anyTemperature(), flowRateM3s(0.02),
				new RawDiameter(lengthMm(0.0)),
				lengthM(50.0), FrictionFactorMethod.SWAMEE_JAIN));
	}

}
