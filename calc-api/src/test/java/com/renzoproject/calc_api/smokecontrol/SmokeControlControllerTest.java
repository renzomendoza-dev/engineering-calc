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

	// --- /plume-tsquared -- fully separate endpoint, sibling to /plume above. ---

	private static final String TSQUARED_URL = "/api/smoke-control/plume-tsquared";

	@Test
	void tSquared_referenceWorkbookExample_matchesCorrectedSpotCheck() throws Exception {
		// alpha = 0.01 kW/s^2, Qcap = 3600 kW, t = 600 s (alpha*t^2 = 3600 -- exactly at the cap
		// boundary), chi = 0.7, ceilingHeight = 2.5 m, fireBaseHeight = 0, To = 30 degC -- same
		// worked example used at the calc-core layer, corrected against the source workbook's
		// undocumented x0.5 bug on the Qc term.
		TSquaredSmokeProductionRequest request = new TSquaredSmokeProductionRequest(0.01, 3600.0, 600.0, 0.7, 2.5, 0.0, 30.0, null);

		TSquaredSmokeProductionResponse response = postForTSquaredResponse(request);

		assertEquals(600.0, response.evaluationTime(), DELTA);
		assertEquals(3600.0, response.designHeatReleaseRate(), DELTA);
		assertEquals(true, response.isGrowthCapped());
		assertEquals(2520.0, response.convectiveHeatReleaseRate(), DELTA);
		assertEquals(3.808, response.flameHeight(), DELTA);
		assertEquals(2.5, response.heightAboveFire(), DELTA);
		assertEquals(TSquaredPlumeRegimeTypeDto.NEAR_FIELD, response.plumeRegime().type());
		assertEquals(8.789, response.plumeRegime().massFlowRate(), DELTA);
		assertEquals(316.75, response.smokeTemperature(), 0.5);
		assertEquals(0.599, response.smokeDensity(), DELTA);
		assertEquals(14.68, response.volumetricFlowRate(), DELTA);
	}

	@Test
	void tSquared_growthBelowCap_isGrowthCappedIsFalse() throws Exception {
		// alpha*t^2 = 0.01*100^2 = 100, well below the 3600 kW cap.
		TSquaredSmokeProductionRequest request = new TSquaredSmokeProductionRequest(0.01, 3600.0, 100.0, 0.7, 2.5, 0.0, 30.0, null);

		TSquaredSmokeProductionResponse response = postForTSquaredResponse(request);

		assertEquals(false, response.isGrowthCapped());
		assertEquals(100.0, response.designHeatReleaseRate(), DELTA);
	}

	@Test
	void tSquared_negativeAmbientTemperature_isStructurallyValid_returns200() throws Exception {
		// Bean Validation has no @Positive on ambientTemperature -- negative Celsius is
		// physically valid.
		TSquaredSmokeProductionRequest request = new TSquaredSmokeProductionRequest(0.01, 3600.0, 600.0, 0.7, 2.5, 0.0, -10.0, null);

		postForTSquaredResponse(request);
	}

	@Test
	void tSquared_nonPositiveFireGrowthRate_returns400() throws Exception {
		TSquaredSmokeProductionRequest request = new TSquaredSmokeProductionRequest(-0.01, 3600.0, 600.0, 0.7, 2.5, 0.0, 30.0, null);

		mockMvc.perform(post(TSQUARED_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void tSquared_negativeEvaluationTime_returns400() throws Exception {
		TSquaredSmokeProductionRequest request = new TSquaredSmokeProductionRequest(0.01, 3600.0, -1.0, 0.7, 2.5, 0.0, 30.0, null);

		mockMvc.perform(post(TSQUARED_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void tSquared_fireBaseHeightAtOrAboveCeilingHeight_isCalcCoreValidation_returns400() throws Exception {
		// Structurally valid at the DTO boundary (both positive), but violates calc-core's
		// domain rule (fireBaseHeight < ceilingHeight) -- must surface as 400 via
		// CalculationException -> GlobalExceptionHandler, not a Bean Validation error.
		TSquaredSmokeProductionRequest request = new TSquaredSmokeProductionRequest(0.01, 3600.0, 600.0, 0.7, 2.5, 2.5, 30.0, null);

		mockMvc.perform(post(TSQUARED_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	private TSquaredSmokeProductionResponse postForTSquaredResponse(TSquaredSmokeProductionRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(TSQUARED_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), TSquaredSmokeProductionResponse.class);
	}

	// --- /vent-area -- standalone endpoint, no coupling to either plume endpoint above. ---

	private static final String VENT_AREA_URL = "/api/smoke-control/vent-area";

	@Test
	void ventArea_warehouse1ValenzuelaCity_matchesApprovedBodReportSpotCheck() throws Exception {
		// V = 64.11 m3/s, Ts = 73.59 degC, To = 35 degC, H = 9.5 m, Cd = 0.6 -- same worked
		// example used at the calc-core layer.
		VentAreaRequest request = new VentAreaRequest(64.11, 73.59, 35.0, 9.5, 0.6);

		VentAreaResponse response = postForVentAreaResponse(request);

		assertEquals(38.59, response.temperatureDifference(), DELTA);
		assertEquals(22.11, response.requiredVentArea(), DELTA);
	}

	@Test
	void ventArea_omittedDischargeCoefficient_fallsBackToCalcCoreDefault() throws Exception {
		VentAreaRequest request = new VentAreaRequest(64.11, 73.59, 35.0, 9.5, null);

		VentAreaResponse response = postForVentAreaResponse(request);

		// Default Cd (0.6) reproduces the same spot-check result.
		assertEquals(22.11, response.requiredVentArea(), DELTA);
	}

	@Test
	void ventArea_negativeSmokeAndAmbientTemperature_isStructurallyValid_returns200() throws Exception {
		// Bean Validation has no @Positive/@PositiveOrZero on either temperature field -- no
		// artificial floor at the API layer.
		VentAreaRequest request = new VentAreaRequest(64.11, -5.0, -20.0, 9.5, 0.6);

		postForVentAreaResponse(request);
	}

	@Test
	void ventArea_missingVolumetricFlowRate_returns400() throws Exception {
		String bodyWithoutFlowRate = """
				{"smokeTemperature":73.59,"ambientTemperature":35.0,"ventHeight":9.5}""";

		mockMvc.perform(post(VENT_AREA_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithoutFlowRate))
				.andExpect(status().isBadRequest());
	}

	@Test
	void ventArea_nonPositiveVentHeight_returns400() throws Exception {
		VentAreaRequest request = new VentAreaRequest(64.11, 73.59, 35.0, 0.0, 0.6);

		mockMvc.perform(post(VENT_AREA_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void ventArea_smokeTemperatureAtOrBelowAmbient_isCalcCoreValidation_returns400() throws Exception {
		// Structurally valid at the DTO boundary (both are just numbers), but violates
		// calc-core's domain rule (deltaT must be strictly positive) -- must surface as 400 via
		// CalculationException -> GlobalExceptionHandler, not a Bean Validation error.
		VentAreaRequest request = new VentAreaRequest(64.11, 35.0, 35.0, 9.5, 0.6);

		mockMvc.perform(post(VENT_AREA_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void ventArea_dischargeCoefficientOutOfRange_isCalcCoreValidation_returns400() throws Exception {
		VentAreaRequest request = new VentAreaRequest(64.11, 73.59, 35.0, 9.5, 1.5);

		mockMvc.perform(post(VENT_AREA_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	private VentAreaResponse postForVentAreaResponse(VentAreaRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(VENT_AREA_URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), VentAreaResponse.class);
	}

}
