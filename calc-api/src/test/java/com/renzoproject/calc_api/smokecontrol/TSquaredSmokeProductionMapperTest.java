package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.smokecontrol.TSquaredFarField;
import com.renzoproject.calc.core.smokecontrol.TSquaredNearField;
import com.renzoproject.calc.core.smokecontrol.TSquaredSmokeProductionInput;
import com.renzoproject.calc.core.smokecontrol.TSquaredSmokeProductionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TSquaredSmokeProductionMapperTest {

	private static final double DELTA = 1e-9;

	@Test
	void toCoreInput_mapsAllFieldsDirectly() {
		TSquaredSmokeProductionRequest request = new TSquaredSmokeProductionRequest(0.01, 3600.0, 600.0, 0.7, 2.5, 0.0, 30.0, 1.0);

		TSquaredSmokeProductionInput input = TSquaredSmokeProductionMapper.toCoreInput(request);

		assertEquals(0.01, input.fireGrowthRate(), DELTA);
		assertEquals(3600.0, input.cappingHRR(), DELTA);
		assertEquals(600.0, input.evaluationTime(), DELTA);
		assertEquals(0.7, input.convectiveFraction(), DELTA);
		assertEquals(2.5, input.ceilingHeight(), DELTA);
		assertEquals(0.0, input.fireBaseHeight(), DELTA);
		assertEquals(30.0, input.ambientTemperature(), DELTA);
		assertEquals(1.0, input.fractionConvectiveHeatInSmokeLayer(), DELTA);
	}

	@Test
	void toCoreInput_omittedOptionalFields_mapToNull() {
		TSquaredSmokeProductionRequest request = new TSquaredSmokeProductionRequest(0.01, 3600.0, 600.0, null, 2.5, null, 30.0, null);

		TSquaredSmokeProductionInput input = TSquaredSmokeProductionMapper.toCoreInput(request);

		assertNull(input.convectiveFraction());
		assertNull(input.fractionConvectiveHeatInSmokeLayer());
		// fireBaseHeight defaults to 0.0 inside TSquaredSmokeProductionInput's own compact constructor.
		assertEquals(0.0, input.fireBaseHeight(), DELTA);
	}

	@Test
	void toResponse_mapsNearFieldRegimeAndGrowthCappedFlag() {
		TSquaredSmokeProductionResult result = new TSquaredSmokeProductionResult(
				600.0, 3600.0, true, 2520.0, 3.808, 2.5, new TSquaredNearField(8.789), 316.75, 0.599, 14.68);

		TSquaredSmokeProductionResponse response = TSquaredSmokeProductionMapper.toResponse(result);

		assertEquals(600.0, response.evaluationTime(), DELTA);
		assertEquals(3600.0, response.designHeatReleaseRate(), DELTA);
		assertTrue(response.isGrowthCapped());
		assertEquals(2520.0, response.convectiveHeatReleaseRate(), DELTA);
		assertEquals(3.808, response.flameHeight(), DELTA);
		assertEquals(2.5, response.heightAboveFire(), DELTA);
		assertEquals(TSquaredPlumeRegimeTypeDto.NEAR_FIELD, response.plumeRegime().type());
		assertEquals(8.789, response.plumeRegime().massFlowRate(), DELTA);
		assertEquals(316.75, response.smokeTemperature(), DELTA);
		assertEquals(0.599, response.smokeDensity(), DELTA);
		assertEquals(14.68, response.volumetricFlowRate(), DELTA);
	}

	@Test
	void toResponse_mapsFarFieldRegimeAndUncappedFlag() {
		TSquaredSmokeProductionResult result = new TSquaredSmokeProductionResult(
				100.0, 100.0, false, 70.0, 1.0, 20.0, new TSquaredFarField(5.0), 50.0, 0.9, 5.5);

		TSquaredSmokeProductionResponse response = TSquaredSmokeProductionMapper.toResponse(result);

		assertFalse(response.isGrowthCapped());
		assertEquals(TSquaredPlumeRegimeTypeDto.FAR_FIELD, response.plumeRegime().type());
		assertEquals(5.0, response.plumeRegime().massFlowRate(), DELTA);
	}

}
