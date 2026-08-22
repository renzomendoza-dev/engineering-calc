package com.renzoproject.calc_api.mechanical.tank;

import com.renzoproject.calc.core.mechanical.pipe.PipeUnits;
import com.renzoproject.calc.core.mechanical.tank.PressureTankInput;
import com.renzoproject.calc.core.mechanical.tank.PressureTankResult;
import com.renzoproject.calc.core.mechanical.tank.PumpControlType;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PressureTankMapperTest {

	private static final double DELTA = 1e-6;

	private static PressureTankRequest baseRequest() {
		return new PressureTankRequest(
				PumpControlTypeDto.CONVENTIONAL,
				40.0,
				1.0,
				140.0,
				280.0,
				0.0);
	}

	@Test
	void toCoreInput_mapsUnitConversionsCorrectly() {
		PressureTankInput input = PressureTankMapper.toCoreInput(baseRequest());

		assertEquals(PumpControlType.CONVENTIONAL, input.controlType());
		// 40 L/min -> 40 L/min round trip.
		assertEquals(40.0, input.drawdownFlowRate().to(PipeUnits.LITRE_PER_MINUTE).getValue().doubleValue(), DELTA);
		assertEquals(1.0, input.minRunTimeMinutes(), DELTA);
		// 140 kPa -> 140000 Pa round trip.
		assertEquals(140000.0, input.lowPressureGauge().to(Units.PASCAL).getValue().doubleValue(), DELTA);
		assertEquals(280000.0, input.highPressureGauge().to(Units.PASCAL).getValue().doubleValue(), DELTA);
		assertEquals(0.0, input.altitude().to(Units.METRE).getValue().doubleValue(), DELTA);
	}

	@Test
	void toCoreInput_vfdControlType_mapsCorrectly() {
		PressureTankRequest request = new PressureTankRequest(
				PumpControlTypeDto.VFD, 5.0, 0.5, 380.0, 400.0, 0.0);

		PressureTankInput input = PressureTankMapper.toCoreInput(request);

		assertEquals(PumpControlType.VFD, input.controlType());
		assertEquals(5.0, input.drawdownFlowRate().to(PipeUnits.LITRE_PER_MINUTE).getValue().doubleValue(), DELTA);
	}

	@Test
	void toResponse_mapsUnitConversionsCorrectly() {
		PressureTankResult result = new PressureTankResult(
				Quantities.getQuantity(40.0, Units.LITRE),
				Quantities.getQuantity(108.92, Units.LITRE),
				Quantities.getQuantity(241325.0, Units.PASCAL),
				Quantities.getQuantity(381325.0, Units.PASCAL));

		PressureTankResponse response = PressureTankMapper.toResponse(result);

		assertEquals(40.0, response.drawdownVolumeLiters(), DELTA);
		assertEquals(108.92, response.requiredTankVolumeLiters(), DELTA);
		// 241325 Pa -> 241.325 kPa round trip.
		assertEquals(241.325, response.lowPressureAbsoluteKpa(), DELTA);
		assertEquals(381.325, response.highPressureAbsoluteKpa(), DELTA);
	}

}
