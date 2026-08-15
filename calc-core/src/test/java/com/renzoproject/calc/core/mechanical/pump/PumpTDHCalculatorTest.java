package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.FlowRegime;
import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossResult;
import com.renzoproject.calc.core.mechanical.pipe.RawDiameter;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PumpTDHCalculatorTest {

	private static final double DELTA = 1e-6;

	private static Quantity<Temperature> anyTemperature() {
		return Quantities.getQuantity(20.0, Units.CELSIUS);
	}

	private static Quantity<com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate> flowRateM3s(double value) {
		return Quantities.getQuantity(value, com.renzoproject.calc.core.mechanical.pipe.PipeUnits.CUBIC_METRE_PER_SECOND);
	}

	private static Quantity<Length> lengthM(double value) {
		return Quantities.getQuantity(value, Units.METRE);
	}

	private static Quantity<Pressure> pascals(double value) {
		return Quantities.getQuantity(value, Units.PASCAL);
	}

	private static PipeSegmentSpec anySegment(double lengthM) {
		return new PipeSegmentSpec(
				new RawDiameter(Quantities.getQuantity(50.0, MetricPrefix.MILLI(Units.METRE))),
				lengthM(lengthM), FrictionFactorMethod.SWAMEE_JAIN);
	}

	private static PipePressureLossResult canned(double headLossM, double velocityMs) {
		return new PipePressureLossResult(
				Quantities.getQuantity(velocityMs, Units.METRE_PER_SECOND),
				100000.0, FlowRegime.TURBULENT, false, 0.02,
				lengthM(headLossM), pascals(1000.0));
	}

	@Test
	void flooded_summsSegmentsAndComputesResidualPressureHead_delegatesToInjectedCalculator() {
		// staticHead = dischargeElevation(15) - suctionHeadFlooded(2) = 13.
		// residualPressureHead = 200000 / (1000*9.80665) = 20.394324259558566 (precomputed).
		// totalDynamicHead = 13 + 0.5(suction) + 2.0(discharge, 1.2+0.8) + 20.394324259558566 + 0(no velocity head).
		StubPipePressureLossCalculator stub = new StubPipePressureLossCalculator(List.of(
				canned(0.5, 1.5),
				canned(1.2, 2.0),
				canned(0.8, 2.5)));
		PumpTDHCalculator calculator = new PumpTDHCalculator(stub, new FakeFluidPropertiesResolver(1000.0, 0.001));

		PipeSegmentSpec suctionSegment = anySegment(10.0);
		PipeSegmentSpec dischargeSegment1 = anySegment(20.0);
		PipeSegmentSpec dischargeSegment2 = anySegment(30.0);

		PumpTDHInput input = new PumpTDHInput(
				"WATER", anyTemperature(), flowRateM3s(0.02),
				SuctionCondition.FLOODED, null, lengthM(2.0), List.of(suctionSegment),
				lengthM(15.0), pascals(200000.0), List.of(dischargeSegment1, dischargeSegment2),
				false);

		PumpTDHResult result = calculator.calculate(input);

		assertEquals(13.0, result.staticHead().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(0.5, result.totalSuctionHeadLoss().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(2.0, result.totalDischargeHeadLoss().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(20.394324259558566, result.residualPressureHead().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(0.0, result.velocityHead().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(35.894324259558566, result.totalDynamicHead().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertFalse(result.staticallyFedWarning());
		assertEquals(1, result.suctionSegmentDetails().size());
		assertEquals(2, result.dischargeSegmentDetails().size());

		// Delegation check: PumpTDHCalculator must have called through to the injected stub with
		// the correct per-segment length/method and the shared fluidKey/temperature/flowRate,
		// not reimplemented any friction math itself.
		assertEquals(3, stub.capturedInputs().size());
		assertEquals(10.0, stub.capturedInputs().get(0).pipeLength().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(20.0, stub.capturedInputs().get(1).pipeLength().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(30.0, stub.capturedInputs().get(2).pipeLength().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals("WATER", stub.capturedInputs().get(0).fluidKey());
		assertEquals(0.02, stub.capturedInputs().get(0).flowRate().to(com.renzoproject.calc.core.mechanical.pipe.PipeUnits.CUBIC_METRE_PER_SECOND).getValue().doubleValue(), DELTA);
	}

	@Test
	void lift_withEmptySuctionSegments_andVelocityHeadOptIn_usesLastDischargeVelocity() {
		// staticHead = dischargeElevation(10) + suctionLift(3) = 13.
		// velocityHead = 3.0^2 / (2*9.80665) = 0.4588722958400677 (precomputed).
		// totalDynamicHead = 13 + 0(suction, empty) + 1.0(discharge) + 0(no residual pressure) + 0.4588722958400677.
		StubPipePressureLossCalculator stub = new StubPipePressureLossCalculator(List.of(canned(1.0, 3.0)));
		PumpTDHCalculator calculator = new PumpTDHCalculator(stub, new FakeFluidPropertiesResolver(1000.0, 0.001));

		PumpTDHInput input = new PumpTDHInput(
				"WATER", anyTemperature(), flowRateM3s(0.02),
				SuctionCondition.LIFT, lengthM(3.0), null, List.of(),
				lengthM(10.0), pascals(0.0), List.of(anySegment(15.0)),
				true);

		PumpTDHResult result = calculator.calculate(input);

		assertEquals(13.0, result.staticHead().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(0.0, result.totalSuctionHeadLoss().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertTrue(result.suctionSegmentDetails().isEmpty());
		assertEquals(0.4588722958400677, result.velocityHead().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(14.458872295840068, result.totalDynamicHead().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertFalse(result.staticallyFedWarning());
	}

	@Test
	void bothSegmentListsEmpty_zeroLossAndNoDetails() {
		StubPipePressureLossCalculator stub = new StubPipePressureLossCalculator(List.of());
		PumpTDHCalculator calculator = new PumpTDHCalculator(stub, new FakeFluidPropertiesResolver(1000.0, 0.001));

		PumpTDHInput input = new PumpTDHInput(
				"WATER", anyTemperature(), flowRateM3s(0.02),
				SuctionCondition.FLOODED, null, lengthM(2.0), List.of(),
				lengthM(15.0), pascals(0.0), List.of(),
				false);

		PumpTDHResult result = calculator.calculate(input);

		assertEquals(0.0, result.totalSuctionHeadLoss().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(0.0, result.totalDischargeHeadLoss().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertTrue(result.suctionSegmentDetails().isEmpty());
		assertTrue(result.dischargeSegmentDetails().isEmpty());
		assertEquals(0, stub.capturedInputs().size());
	}

	@Test
	void negativeTotalDynamicHead_setsStaticallyFedWarningRatherThanThrowing() {
		// staticHead = dischargeElevation(1) - suctionHeadFlooded(10) = -9. No segments, no
		// residual pressure -> totalDynamicHead = -9 <= 0.
		StubPipePressureLossCalculator stub = new StubPipePressureLossCalculator(List.of());
		PumpTDHCalculator calculator = new PumpTDHCalculator(stub, new FakeFluidPropertiesResolver(1000.0, 0.001));

		PumpTDHInput input = new PumpTDHInput(
				"WATER", anyTemperature(), flowRateM3s(0.02),
				SuctionCondition.FLOODED, null, lengthM(10.0), List.of(),
				lengthM(1.0), pascals(0.0), List.of(),
				false);

		PumpTDHResult result = calculator.calculate(input);

		assertEquals(-9.0, result.totalDynamicHead().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertTrue(result.staticallyFedWarning());
	}

	@Test
	void velocityHeadRequested_withEmptyDischargeSegments_throws() {
		StubPipePressureLossCalculator stub = new StubPipePressureLossCalculator(List.of());
		PumpTDHCalculator calculator = new PumpTDHCalculator(stub, new FakeFluidPropertiesResolver(1000.0, 0.001));

		PumpTDHInput input = new PumpTDHInput(
				"WATER", anyTemperature(), flowRateM3s(0.02),
				SuctionCondition.FLOODED, null, lengthM(2.0), List.of(),
				lengthM(15.0), pascals(0.0), List.of(),
				true);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

	@Test
	void nestedCalculationException_propagatesWithSegmentContext() {
		ThrowingStubPipePressureLossCalculator stub = new ThrowingStubPipePressureLossCalculator("no such nominal size");
		PumpTDHCalculator calculator = new PumpTDHCalculator(stub, new FakeFluidPropertiesResolver(1000.0, 0.001));

		PumpTDHInput input = new PumpTDHInput(
				"WATER", anyTemperature(), flowRateM3s(0.02),
				SuctionCondition.FLOODED, null, lengthM(2.0), List.of(anySegment(10.0)),
				lengthM(15.0), pascals(0.0), List.of(),
				false);

		CalculationException thrown = assertThrows(CalculationException.class, () -> calculator.calculate(input));
		assertTrue(thrown.getMessage().contains("no such nominal size"));
		assertTrue(thrown.getMessage().contains("suction"));
	}

	@Test
	void floodedWithSuctionLiftAlsoProvided_throws() {
		assertThrows(CalculationException.class, () -> new PumpTDHInput(
				"WATER", anyTemperature(), flowRateM3s(0.02),
				SuctionCondition.FLOODED, lengthM(3.0), lengthM(2.0), List.of(),
				lengthM(15.0), pascals(0.0), List.of(), false));
	}

	@Test
	void liftWithoutSuctionLift_throws() {
		assertThrows(CalculationException.class, () -> new PumpTDHInput(
				"WATER", anyTemperature(), flowRateM3s(0.02),
				SuctionCondition.LIFT, null, null, List.of(),
				lengthM(15.0), pascals(0.0), List.of(), false));
	}

	@Test
	void nonPositiveFlowRate_throws() {
		assertThrows(CalculationException.class, () -> new PumpTDHInput(
				"WATER", anyTemperature(), flowRateM3s(0.0),
				SuctionCondition.FLOODED, null, lengthM(2.0), List.of(),
				lengthM(15.0), pascals(0.0), List.of(), false));
	}

}
