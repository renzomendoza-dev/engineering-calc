package com.renzoproject.calc_api.mechanical.duct;

import com.renzoproject.calc.core.mechanical.duct.DuctShape;
import com.renzoproject.calc.core.mechanical.duct.DuctSizingInput;
import com.renzoproject.calc.core.mechanical.duct.DuctSizingMethod;
import com.renzoproject.calc.core.mechanical.duct.DuctSizingResult;
import com.renzoproject.calc.core.mechanical.duct.FixedDimensionType;
import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import com.renzoproject.calc_api.mechanical.pipe.FrictionFactorMethodDto;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DuctSizingMapperTest {

	private static final double DELTA = 1e-6;

	@Test
	void toCoreInput_round_velocity_mapsFieldsAndLeavesShapeAndMethodFieldsNull() {
		DuctSizingRequest request = new DuctSizingRequest(
				DuctSizingMethodDto.VELOCITY, 500.0, 20.0, 0.0, "TESTMAT", FrictionFactorMethodDto.SWAMEE_JAIN,
				DuctShapeDto.ROUND, null, null, null, 5.0);

		DuctSizingInput input = DuctSizingMapper.toCoreInput(request);

		assertEquals(DuctSizingMethod.VELOCITY, input.method());
		assertEquals(DuctShape.ROUND, input.shape());
		assertEquals("TESTMAT", input.ductMaterial());
		assertEquals(500.0, input.airFlow().to(PipeUnits.LITRE_PER_SECOND).getValue().doubleValue(), DELTA);
		assertEquals(20.0, input.airTemperature().to(Units.CELSIUS).getValue().doubleValue(), DELTA);
		assertEquals(0.0, input.altitude().to(Units.METRE).getValue().doubleValue(), DELTA);
		assertEquals(5.0, input.maxVelocity().to(Units.METRE_PER_SECOND).getValue().doubleValue(), DELTA);
		assertNull(input.fixedDimensionType());
		assertNull(input.fixedDimensionValue());
		assertNull(input.targetFrictionRatePerMeter());
	}

	@Test
	void toCoreInput_rectangular_equalFriction_mapsAllFieldsIncludingMmConversion() {
		DuctSizingRequest request = new DuctSizingRequest(
				DuctSizingMethodDto.EQUAL_FRICTION, 500.0, 20.0, 0.0, "TESTMAT", FrictionFactorMethodDto.COLEBROOK_WHITE,
				DuctShapeDto.RECTANGULAR, FixedDimensionTypeDto.HEIGHT, 300.0, 1.0, null);

		DuctSizingInput input = DuctSizingMapper.toCoreInput(request);

		assertEquals(DuctSizingMethod.EQUAL_FRICTION, input.method());
		assertEquals(DuctShape.RECTANGULAR, input.shape());
		assertEquals(FixedDimensionType.HEIGHT, input.fixedDimensionType());
		// 300mm -> 0.3m round trip.
		assertEquals(0.3, input.fixedDimensionValue().to(Units.METRE).getValue().doubleValue(), DELTA);
		// 1.0 Pa/m maps to a Quantity<Pressure> of 1.0 Pa, matching calc-core's own contract.
		assertEquals(1.0, input.targetFrictionRatePerMeter().to(Units.PASCAL).getValue().doubleValue(), DELTA);
		assertNull(input.maxVelocity());
	}

	@Test
	void toResponse_round_mapsMillimetreAndPascalFieldsWithNullWidthHeight() {
		DuctSizingResult result = new DuctSizingResult(
				Quantities.getQuantity(0.5, Units.METRE),
				null,
				null,
				Quantities.getQuantity(5.0, Units.METRE_PER_SECOND),
				250000.0,
				0.02,
				Quantities.getQuantity(1.0, Units.PASCAL));

		DuctSizingResponse response = DuctSizingMapper.toResponse(result);

		assertEquals(500.0, response.equivalentDiameterMm(), DELTA);
		assertNull(response.ductWidthMm());
		assertNull(response.ductHeightMm());
		assertEquals(5.0, response.actualVelocityMps(), DELTA);
		assertEquals(250000.0, response.reynoldsNumber(), DELTA);
		assertEquals(0.02, response.frictionFactor(), DELTA);
		assertEquals(1.0, response.actualFrictionRatePaPerM(), DELTA);
	}

	@Test
	void toResponse_rectangular_mapsWidthAndHeightToMillimetres() {
		DuctSizingResult result = new DuctSizingResult(
				Quantities.getQuantity(0.42, Units.METRE),
				Quantities.getQuantity(0.6, Units.METRE),
				Quantities.getQuantity(0.3, Units.METRE),
				Quantities.getQuantity(4.5, Units.METRE_PER_SECOND),
				200000.0,
				0.021,
				Quantities.getQuantity(0.95, Units.PASCAL));

		DuctSizingResponse response = DuctSizingMapper.toResponse(result);

		assertEquals(420.0, response.equivalentDiameterMm(), DELTA);
		assertEquals(600.0, response.ductWidthMm(), DELTA);
		assertEquals(300.0, response.ductHeightMm(), DELTA);
	}

}
