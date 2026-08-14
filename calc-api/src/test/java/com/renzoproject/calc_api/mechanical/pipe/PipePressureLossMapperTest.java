package com.renzoproject.calc_api.mechanical.pipe;

import com.renzoproject.calc.core.mechanical.pipe.FlowRegime;
import com.renzoproject.calc.core.mechanical.pipe.FrictionFactorMethod;
import com.renzoproject.calc.core.mechanical.pipe.NominalSize;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossInput;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossResult;
import com.renzoproject.calc.core.mechanical.pipe.RawDiameter;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PipePressureLossMapperTest {

	private static final double DELTA = 1e-9;

	@Test
	void toCoreInput_nominalDiameterSpec_buildsNominalSizeAndConvertsAllFields() {
		PipePressureLossRequest request = new PipePressureLossRequest(
				"WATER", 20.0, 5.0, "L/s",
				DiameterSpecTypeDto.NOMINAL, "GI", "SCH40", "2", null, null,
				100.0, FrictionFactorMethodDto.SWAMEE_JAIN);

		PipePressureLossInput input = PipePressureLossMapper.toCoreInput(request);

		assertEquals("WATER", input.fluidKey());
		assertEquals(20.0, input.fluidTemperature().to(Units.CELSIUS).getValue().doubleValue(), DELTA);
		// 5.0 L/s = 0.005 m3/s
		assertEquals(0.005, input.flowRate().to(com.renzoproject.calc.core.mechanical.pipe.PipeUnits.CUBIC_METRE_PER_SECOND).getValue().doubleValue(), DELTA);
		assertInstanceOf(NominalSize.class, input.diameterSpec());
		NominalSize nominalSize = (NominalSize) input.diameterSpec();
		assertEquals("GI", nominalSize.material());
		assertEquals("SCH40", nominalSize.schedule());
		assertEquals("2", nominalSize.nominalLabel());
		assertEquals(100.0, input.pipeLength().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(FrictionFactorMethod.SWAMEE_JAIN, input.method());
	}

	@Test
	void toCoreInput_rawDiameterSpec_buildsRawDiameterConvertedToMetres() {
		PipePressureLossRequest request = new PipePressureLossRequest(
				"WATER", 20.0, 0.005, "m3/s",
				DiameterSpecTypeDto.RAW, null, null, null, 52.48, "mm",
				100.0, FrictionFactorMethodDto.COLEBROOK_WHITE);

		PipePressureLossInput input = PipePressureLossMapper.toCoreInput(request);

		assertInstanceOf(RawDiameter.class, input.diameterSpec());
		RawDiameter rawDiameter = (RawDiameter) input.diameterSpec();
		assertEquals(0.05248, rawDiameter.internalDiameter().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(FrictionFactorMethod.COLEBROOK_WHITE, input.method());
	}

	@Test
	void toResponse_mapsAllFieldsFlat() {
		PipePressureLossResult result = new PipePressureLossResult(
				Quantities.getQuantity(2.31, Units.METRE_PER_SECOND),
				120847.07,
				FlowRegime.TURBULENT,
				false,
				0.0269,
				Quantities.getQuantity(13.98, Units.METRE),
				Quantities.getQuantity(136897.42, Units.PASCAL));

		PipePressureLossResponse response = PipePressureLossMapper.toResponse(result);

		assertEquals(2.31, response.velocityMetersPerSecond(), DELTA);
		assertEquals(120847.07, response.reynoldsNumber(), DELTA);
		assertEquals(FlowRegimeDto.TURBULENT, response.flowRegime());
		assertEquals(false, response.transitionalRegimeWarning());
		assertEquals(0.0269, response.frictionFactor(), DELTA);
		assertEquals(13.98, response.headLossMeters(), DELTA);
		assertEquals(136897.42, response.pressureLossPascals(), DELTA);
	}

	@Test
	void toResponse_transitionalRegimeWarningPreserved() {
		PipePressureLossResult result = new PipePressureLossResult(
				Quantities.getQuantity(1.0, Units.METRE_PER_SECOND),
				3000.0,
				FlowRegime.TRANSITIONAL,
				true,
				0.045,
				Quantities.getQuantity(1.0, Units.METRE),
				Quantities.getQuantity(1000.0, Units.PASCAL));

		PipePressureLossResponse response = PipePressureLossMapper.toResponse(result);

		assertEquals(FlowRegimeDto.TRANSITIONAL, response.flowRegime());
		assertEquals(true, response.transitionalRegimeWarning());
	}

}
