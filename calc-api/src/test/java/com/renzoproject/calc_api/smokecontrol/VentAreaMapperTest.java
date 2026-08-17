package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.smokecontrol.VentAreaInput;
import com.renzoproject.calc.core.smokecontrol.VentAreaResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VentAreaMapperTest {

	private static final double DELTA = 1e-9;

	@Test
	void toCoreInput_mapsAllFieldsDirectly() {
		VentAreaRequest request = new VentAreaRequest(64.11, 73.59, 35.0, 9.5, 0.6);

		VentAreaInput input = VentAreaMapper.toCoreInput(request);

		assertEquals(64.11, input.volumetricFlowRate(), DELTA);
		assertEquals(73.59, input.smokeTemperature(), DELTA);
		assertEquals(35.0, input.ambientTemperature(), DELTA);
		assertEquals(9.5, input.ventHeight(), DELTA);
		assertEquals(0.6, input.dischargeCoefficient(), DELTA);
	}

	@Test
	void toCoreInput_omittedDischargeCoefficient_mapsToNull() {
		VentAreaRequest request = new VentAreaRequest(64.11, 73.59, 35.0, 9.5, null);

		VentAreaInput input = VentAreaMapper.toCoreInput(request);

		assertNull(input.dischargeCoefficient());
	}

	@Test
	void toResponse_mapsBothFieldsDirectly() {
		VentAreaResult result = new VentAreaResult(38.59, 22.11);

		VentAreaResponse response = VentAreaMapper.toResponse(result);

		assertEquals(38.59, response.temperatureDifference(), DELTA);
		assertEquals(22.11, response.requiredVentArea(), DELTA);
	}

}
