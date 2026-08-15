package com.renzoproject.calc_api.smokecontrol;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SmokeControlControllerTest {

	private static final String URL = "/api/smoke-control/plume";
	private static final double DELTA = 1e-2;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void warehouse1ValenzuelaCity_matchesRealProjectSpotCheck() throws Exception {
		// A = 9 m2 (derived from Q=3600kW / HRR=400kW/m2), HRR = 400 kW/m2, chi = 0.7,
		// ceilingHeight = 12 m, fireBaseHeight = 0, To = 35 degC -- same worked example used at
		// the calc-core layer.
		SmokeProductionRequest request = new SmokeProductionRequest(9.0, 400.0, 0.7, 12.0, 0.0, 35.0, null);

		SmokeProductionResponse response = postForResponse(request);

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
	void omittedOptionalFields_fallBackToCalcCoreDefaults_returns200() throws Exception {
		SmokeProductionRequest request = new SmokeProductionRequest(9.0, 400.0, null, 12.0, null, 35.0, null);

		SmokeProductionResponse response = postForResponse(request);

		// Defaults (chi=0.7, Ks=1.0) reproduce the same spot-check result.
		assertEquals(2520.0, response.convectiveHeatReleaseRate(), DELTA);
	}

	@Test
	void missingDesignFireArea_returns400() throws Exception {
		String bodyWithoutArea = """
				{"heatReleaseRateDensity":400.0,"ceilingHeight":12.0,"ambientTemperature":35.0}""";

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithoutArea))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nonPositiveDesignFireArea_returns400() throws Exception {
		SmokeProductionRequest request = new SmokeProductionRequest(-9.0, 400.0, 0.7, 12.0, 0.0, 35.0, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void negativeAmbientTemperature_isStructurallyValid_returns200() throws Exception {
		// Bean Validation has no @Positive on ambientTemperature -- negative Celsius is
		// physically valid, so this should reach calc-core, not 400 at the DTO boundary.
		SmokeProductionRequest request = new SmokeProductionRequest(9.0, 400.0, 0.7, 12.0, 0.0, -10.0, null);

		postForResponse(request);
	}

	@Test
	void fireBaseHeightAtOrAboveCeilingHeight_isCalcCoreValidation_returns400() throws Exception {
		// Structurally valid at the DTO boundary (both positive), but violates calc-core's
		// domain rule (fireBaseHeight < ceilingHeight) -- must surface as 400 via
		// CalculationException -> GlobalExceptionHandler, not a Bean Validation error.
		SmokeProductionRequest request = new SmokeProductionRequest(9.0, 400.0, 0.7, 12.0, 12.0, 35.0, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	private SmokeProductionResponse postForResponse(SmokeProductionRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), SmokeProductionResponse.class);
	}

}
