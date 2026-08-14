package com.renzoproject.calc_api.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.DiameterSizingResult;
import com.renzoproject.calc.core.mechanical.pipe.NominalSize;
import com.renzoproject.calc.core.mechanical.pipe.PipeSizingMode;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityInput;
import com.renzoproject.calc.core.mechanical.pipe.RawDiameter;
import com.renzoproject.calc.core.mechanical.pipe.VelocityResult;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PipeVelocityMapperTest {

	private static final double DELTA = 1e-9;

	@Test
	void toCoreInput_nominalDiameterSpec_buildsNominalSizeAndConvertsFlowRate() {
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.VELOCITY_FROM_DIAMETER, 2.0, "L/s",
				DiameterSpecTypeDto.NOMINAL, "GI", "SCH40", "2",
				null, null,
				null, null, null, null);

		PipeVelocityInput input = PipeVelocityMapper.toCoreInput(request);

		assertEquals(PipeSizingMode.VELOCITY_FROM_DIAMETER, input.mode());
		// 2.0 L/s = 0.002 m3/s
		assertEquals(0.002, input.flowRate().to(com.renzoproject.calc.core.mechanical.pipe.PipeUnits.CUBIC_METRE_PER_SECOND).getValue().doubleValue(), DELTA);
		assertInstanceOf(NominalSize.class, input.diameterSpec());
		NominalSize nominalSize = (NominalSize) input.diameterSpec();
		assertEquals("GI", nominalSize.material());
		assertEquals("SCH40", nominalSize.schedule());
		assertEquals("2", nominalSize.nominalLabel());
		assertNull(input.targetVelocity());
	}

	@Test
	void toCoreInput_rawDiameterSpec_convertsToMetres() {
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.VELOCITY_FROM_DIAMETER, 0.002, "m3/s",
				DiameterSpecTypeDto.RAW, null, null, null,
				50.0, "mm",
				null, null, null, null);

		PipeVelocityInput input = PipeVelocityMapper.toCoreInput(request);

		assertInstanceOf(RawDiameter.class, input.diameterSpec());
		RawDiameter rawDiameter = (RawDiameter) input.diameterSpec();
		assertEquals(0.05, rawDiameter.internalDiameter().to(Units.METRE).getValue().doubleValue(), DELTA);
	}

	@Test
	void toCoreInput_diameterFromVelocityMode_setsTargetVelocityMaterialAndSchedule() {
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.DIAMETER_FROM_VELOCITY, 0.01, "m3/s",
				null, null, null, null,
				null, null,
				1.5, "m/s", "GI", "SCH40");

		PipeVelocityInput input = PipeVelocityMapper.toCoreInput(request);

		assertEquals(PipeSizingMode.DIAMETER_FROM_VELOCITY, input.mode());
		assertNull(input.diameterSpec());
		assertEquals(1.5, input.targetVelocity().to(Units.METRE_PER_SECOND).getValue().doubleValue(), DELTA);
		assertEquals("GI", input.pipeMaterial());
		assertEquals("SCH40", input.schedule());
	}

	@Test
	void toCoreInput_flowRateInGallonsPerMinute_convertsToCubicMetresPerSecond() {
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.VELOCITY_FROM_DIAMETER, 100.0, "gpm",
				DiameterSpecTypeDto.RAW, null, null, null,
				50.0, "mm",
				null, null, null, null);

		PipeVelocityInput input = PipeVelocityMapper.toCoreInput(request);

		// 100 US gpm = 100 * 3.785411784 L / 60 s = 6.30901964 L/s = 0.00630901964 m3/s.
		assertEquals(0.00630901964, input.flowRate().to(com.renzoproject.calc.core.mechanical.pipe.PipeUnits.CUBIC_METRE_PER_SECOND).getValue().doubleValue(), DELTA);
	}

	@Test
	void toCoreInput_targetVelocityInFeetPerSecond_convertsToMetresPerSecond() {
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.DIAMETER_FROM_VELOCITY, 0.01, "m3/s",
				null, null, null, null,
				null, null,
				10.0, "ft/s", "GI", "SCH40");

		PipeVelocityInput input = PipeVelocityMapper.toCoreInput(request);

		// 10 ft/s = 3.048 m/s.
		assertEquals(3.048, input.targetVelocity().to(Units.METRE_PER_SECOND).getValue().doubleValue(), DELTA);
	}

	@Test
	void toCoreInput_unknownFlowRateUnit_throws() {
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.VELOCITY_FROM_DIAMETER, 1.0, "gallons/fortnight",
				DiameterSpecTypeDto.RAW, null, null, null,
				50.0, "mm",
				null, null, null, null);

		assertThrows(CalculationException.class, () -> PipeVelocityMapper.toCoreInput(request));
	}

	@Test
	void toCoreInput_unknownLengthUnit_throws() {
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.VELOCITY_FROM_DIAMETER, 1.0, "m3/s",
				DiameterSpecTypeDto.RAW, null, null, null,
				50.0, "furlongs",
				null, null, null, null);

		assertThrows(CalculationException.class, () -> PipeVelocityMapper.toCoreInput(request));
	}

	@Test
	void toCoreInput_unknownSpeedUnit_throws() {
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.DIAMETER_FROM_VELOCITY, 0.01, "m3/s",
				null, null, null, null,
				null, null,
				1.5, "furlongs/fortnight", "GI", "SCH40");

		assertThrows(CalculationException.class, () -> PipeVelocityMapper.toCoreInput(request));
	}

	@Test
	void toResponse_velocityResult_populatesOnlyVelocityFields() {
		VelocityResult result = new VelocityResult(Quantities.getQuantity(1.2, Units.METRE_PER_SECOND));

		PipeVelocityResponse response = PipeVelocityMapper.toResponse(result);

		assertEquals(PipeSizingModeDto.VELOCITY_FROM_DIAMETER, response.mode());
		assertEquals(1.2, response.velocityValue(), DELTA);
		assertEquals("m/s", response.velocityUnit());
		assertNull(response.calculatedMinDiameterValue());
		assertNull(response.calculatedMinDiameterUnit());
		assertNull(response.nominalPipeSize());
		assertNull(response.actualInternalDiameterValue());
		assertNull(response.actualInternalDiameterUnit());
		assertNull(response.actualVelocityValue());
		assertNull(response.actualVelocityUnit());
	}

	@Test
	void toResponse_diameterSizingResult_populatesOnlyDesignModeFields() {
		DiameterSizingResult result = new DiameterSizingResult(
				Quantities.getQuantity(0.0921, Units.METRE),
				"2\" (DN50)",
				Quantities.getQuantity(52.48, MetricPrefix.MILLI(Units.METRE)),
				Quantities.getQuantity(1.27, Units.METRE_PER_SECOND));

		PipeVelocityResponse response = PipeVelocityMapper.toResponse(result);

		assertEquals(PipeSizingModeDto.DIAMETER_FROM_VELOCITY, response.mode());
		assertNull(response.velocityValue());
		assertNull(response.velocityUnit());
		assertEquals(92.1, response.calculatedMinDiameterValue(), 0.01);
		assertEquals("mm", response.calculatedMinDiameterUnit());
		assertEquals("2\" (DN50)", response.nominalPipeSize());
		assertEquals(52.48, response.actualInternalDiameterValue(), DELTA);
		assertEquals("mm", response.actualInternalDiameterUnit());
		assertEquals(1.27, response.actualVelocityValue(), DELTA);
		assertEquals("m/s", response.actualVelocityUnit());
	}

}
