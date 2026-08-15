package com.renzoproject.calc_api.acoustics;

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
class FireAlarmAudibilityControllerTest {

	private static final String URL = "/api/acoustics/fire-alarm-audibility";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void publicMode_workedExample_averageAmbient50_requiredThreshold65_passesWhenApplianceLoudEnough() throws Exception {
		// Core spot-check worked example: average ambient 50 dB, public mode -> required
		// threshold 65 dB (50 + 15). Appliance rated at 70 dB with no attenuation (same
		// reference/target distance) -> 70 >= 65 -> PASS.
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.PUBLIC, 70.0, 1.0, 1.0, 50.0, null);

		FireAlarmAudibilityResponse response = postForResponse(request);

		assertEquals(70.0, response.calculatedTargetSplDb(), DELTA);
		assertEquals(65.0, response.requiredThresholdDb(), DELTA);
		assertEquals(GoverningAudibilityRuleDto.AVERAGE_AMBIENT_PLUS_OFFSET, response.governingRule());
		assertEquals(AudibilityOutcomeDto.PASS, response.outcome());
	}

	@Test
	void publicMode_workedExample_appliancesTooQuiet_fails() throws Exception {
		// Same worked example (required threshold 65 dB), but the appliance is only rated at
		// 60 dB -- below threshold -> FAIL, not an error response.
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.PUBLIC, 60.0, 1.0, 1.0, 50.0, null);

		FireAlarmAudibilityResponse response = postForResponse(request);

		assertEquals(65.0, response.requiredThresholdDb(), DELTA);
		assertEquals(AudibilityOutcomeDto.FAIL, response.outcome());
	}

	@Test
	void publicMode_applianceExceedsSystemWideMax_isDesignConflictNot400() throws Exception {
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.PUBLIC, 120.0, 1.0, 1.0, 50.0, null);

		FireAlarmAudibilityResponse response = postForResponse(request);

		assertEquals(AudibilityOutcomeDto.EXCEEDS_MAX_LIMIT, response.outcome());
	}

	@Test
	void privateMode_usesNarrowerOffset_returns200() throws Exception {
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.PRIVATE, 60.0, 1.0, 1.0, 50.0, null);

		FireAlarmAudibilityResponse response = postForResponse(request);

		// private mode offset is 10 dB, not public's 15 -> required threshold 60.
		assertEquals(60.0, response.requiredThresholdDb(), DELTA);
		assertEquals(AudibilityOutcomeDto.PASS, response.outcome());
	}

	@Test
	void sleepingMode_lowAmbient_absoluteFloorGoverns_returns200() throws Exception {
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.SLEEPING, 75.0, 1.0, 1.0, 40.0, null);

		FireAlarmAudibilityResponse response = postForResponse(request);

		assertEquals(75.0, response.requiredThresholdDb(), DELTA);
		assertEquals(GoverningAudibilityRuleDto.ABSOLUTE_SLEEPING_FLOOR, response.governingRule());
		assertEquals(AudibilityOutcomeDto.PASS, response.outcome());
	}

	@Test
	void missingMode_returns400() throws Exception {
		String bodyWithoutMode = """
				{"applianceSplAtReferenceDb":70.0,"referenceDistanceMeters":1.0,"targetDistanceMeters":1.0,"measuredAverageAmbientDb":50.0}""";

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithoutMode))
				.andExpect(status().isBadRequest());
	}

	@Test
	void negativeApplianceSpl_returns400() throws Exception {
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.PUBLIC, -1.0, 1.0, 1.0, 50.0, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nonPositiveReferenceDistance_returns400() throws Exception {
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.PUBLIC, 70.0, 0.0, 1.0, 50.0, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void negativeMeasuredMaxSustainedAmbient_returns400() throws Exception {
		FireAlarmAudibilityRequest request = new FireAlarmAudibilityRequest(FireAlarmNotificationMode.PUBLIC, 70.0, 1.0, 1.0, 50.0, -5.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	private FireAlarmAudibilityResponse postForResponse(FireAlarmAudibilityRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), FireAlarmAudibilityResponse.class);
	}

}
