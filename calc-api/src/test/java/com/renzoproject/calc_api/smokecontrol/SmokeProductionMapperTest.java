package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.smokecontrol.FarField;
import com.renzoproject.calc.core.smokecontrol.NearField;
import com.renzoproject.calc.core.smokecontrol.SmokeProductionInput;
import com.renzoproject.calc.core.smokecontrol.SmokeProductionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SmokeProductionMapperTest {

	private static final double DELTA = 1e-9;

	@Test
	void toCoreInput_mapsAllFieldsDirectly() {
		SmokeProductionRequest request = new SmokeProductionRequest(9.0, 400.0, 0.7, 12.0, 0.0, 35.0, 1.0);

		SmokeProductionInput input = SmokeProductionMapper.toCoreInput(request);

		assertEquals(9.0, input.designFireArea(), DELTA);
		assertEquals(400.0, input.heatReleaseRateDensity(), DELTA);
		assertEquals(0.7, input.convectiveFraction(), DELTA);
		assertEquals(12.0, input.ceilingHeight(), DELTA);
		assertEquals(0.0, input.fireBaseHeight(), DELTA);
		assertEquals(35.0, input.ambientTemperature(), DELTA);
		assertEquals(1.0, input.fractionConvectiveHeatInSmokeLayer(), DELTA);
	}

	@Test
	void toCoreInput_omittedOptionalFields_mapToNull() {
		SmokeProductionRequest request = new SmokeProductionRequest(9.0, 400.0, null, 12.0, null, 35.0, null);

		SmokeProductionInput input = SmokeProductionMapper.toCoreInput(request);

		assertNull(input.convectiveFraction());
		assertNull(input.fractionConvectiveHeatInSmokeLayer());
		// fireBaseHeight defaults to 0.0 inside SmokeProductionInput's own compact constructor.
		assertEquals(0.0, input.fireBaseHeight(), DELTA);
	}

	@Test
	void toResponse_mapsFarFieldRegime() {
		SmokeProductionResult result = new SmokeProductionResult(3600.0, 2520.0, 3.81, 12.0, new FarField(65.31), 73.59, 1.019, 64.11);

		SmokeProductionResponse response = SmokeProductionMapper.toResponse(result);

		assertEquals(3600.0, response.designHeatReleaseRate(), DELTA);
		assertEquals(2520.0, response.convectiveHeatReleaseRate(), DELTA);
		assertEquals(3.81, response.flameHeight(), DELTA);
		assertEquals(12.0, response.heightAboveFire(), DELTA);
		assertEquals(PlumeRegimeTypeDto.FAR_FIELD, response.plumeRegime().type());
		assertEquals(65.31, response.plumeRegime().massFlowRate(), DELTA);
		assertEquals(73.59, response.smokeTemperature(), DELTA);
		assertEquals(1.019, response.smokeDensity(), DELTA);
		assertEquals(64.11, response.volumetricFlowRate(), DELTA);
	}

	@Test
	void toResponse_mapsNearFieldRegime() {
		SmokeProductionResult result = new SmokeProductionResult(50000.0, 35000.0, 10.9, 5.0, new NearField(85.2), 200.0, 0.7, 121.7);

		SmokeProductionResponse response = SmokeProductionMapper.toResponse(result);

		assertEquals(PlumeRegimeTypeDto.NEAR_FIELD, response.plumeRegime().type());
		assertEquals(85.2, response.plumeRegime().massFlowRate(), DELTA);
	}

}
