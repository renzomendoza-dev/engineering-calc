package com.renzoproject.calc_api.acoustics;

import com.renzoproject.calc.core.acoustics.DistanceAttenuationInput;
import com.renzoproject.calc.core.acoustics.DistanceAttenuationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistanceAttenuationMapperTest {

	private static final double DELTA = 1e-9;

	@Test
	void toCoreInput_mapsAllFieldsDirectly() {
		DistanceAttenuationRequest request = new DistanceAttenuationRequest(100.0, 1.0, 2.0);

		DistanceAttenuationInput input = DistanceAttenuationMapper.toCoreInput(request);

		assertEquals(100.0, input.referenceSplDb(), DELTA);
		assertEquals(1.0, input.referenceDistance(), DELTA);
		assertEquals(2.0, input.targetDistance(), DELTA);
	}

	@Test
	void toResponse_mapsBothFieldsDirectly() {
		DistanceAttenuationResult result = new DistanceAttenuationResult(93.97940008672038, -6.020599913279624);

		DistanceAttenuationResponse response = DistanceAttenuationMapper.toResponse(result);

		assertEquals(93.97940008672038, response.targetSplDb(), DELTA);
		assertEquals(-6.020599913279624, response.attenuationDb(), DELTA);
	}

}
