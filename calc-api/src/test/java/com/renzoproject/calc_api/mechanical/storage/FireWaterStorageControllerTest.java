package com.renzoproject.calc_api.mechanical.storage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FireWaterStorageControllerTest {

	private static final String URL = "/api/mechanical/storage/fire";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void omittedSelectedDuration_defaultsToConservativeMax() throws Exception {
		// Real fire-water-duration.json: Ordinary Hazard is 60-90 minutes.
		FireWaterStorageRequest request = new FireWaterStorageRequest(500.0, HazardClassificationDto.ORDINARY_HAZARD, null, 0.0);

		FireWaterStorageResponse response = postForResponse(request);

		assertEquals(60.0, response.resolvedDurationMinutesMin(), DELTA);
		assertEquals(90.0, response.resolvedDurationMinutesMax(), DELTA);
		assertEquals(90.0, response.durationMinutesUsed(), DELTA);
		assertTrue(response.usedConservativeDefault());
		assertEquals(500.0 * 90.0, response.requiredStorageVolumeGallons(), DELTA);
	}

	@Test
	void inRangeSelectedDuration_isUsedDirectly() throws Exception {
		FireWaterStorageRequest request = new FireWaterStorageRequest(500.0, HazardClassificationDto.ORDINARY_HAZARD, 75.0, 0.0);

		FireWaterStorageResponse response = postForResponse(request);

		assertEquals(75.0, response.durationMinutesUsed(), DELTA);
		assertFalse(response.usedConservativeDefault());
		assertEquals(500.0 * 75.0, response.requiredStorageVolumeGallons(), DELTA);
	}

	@Test
	void outOfRangeSelectedDuration_returns400WithValidRangeInMessage() throws Exception {
		// 50 minutes is below Ordinary Hazard's 60-minute minimum.
		FireWaterStorageRequest request = new FireWaterStorageRequest(500.0, HazardClassificationDto.ORDINARY_HAZARD, 50.0, 0.0);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andReturn();

		String body = mvcResult.getResponse().getContentAsString();
		assertTrue(body.contains("60"), "Error message should state the valid range's minimum: " + body);
		assertTrue(body.contains("90"), "Error message should state the valid range's maximum: " + body);
	}

	@Test
	void lightHazard_fixedThirtyMinutes_nullAndExplicitAgree() throws Exception {
		FireWaterStorageResponse nullResponse = postForResponse(
				new FireWaterStorageRequest(100.0, HazardClassificationDto.LIGHT_HAZARD, null, 0.0));
		FireWaterStorageResponse explicitResponse = postForResponse(
				new FireWaterStorageRequest(100.0, HazardClassificationDto.LIGHT_HAZARD, 30.0, 0.0));

		assertEquals(30.0, nullResponse.durationMinutesUsed(), DELTA);
		assertTrue(nullResponse.usedConservativeDefault());
		assertEquals(30.0, explicitResponse.durationMinutesUsed(), DELTA);
		assertFalse(explicitResponse.usedConservativeDefault());
		assertEquals(nullResponse.requiredStorageVolumeGallons(), explicitResponse.requiredStorageVolumeGallons(), DELTA);
	}

	@Test
	void missingRatedPumpFlow_returns400() throws Exception {
		String bodyWithoutFlow = """
				{"hazardClassification":"LIGHT_HAZARD"}""";

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithoutFlow))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nonPositiveRatedPumpFlow_returns400() throws Exception {
		FireWaterStorageRequest request = new FireWaterStorageRequest(-500.0, HazardClassificationDto.LIGHT_HAZARD, null, 0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingHazardClassification_returns400() throws Exception {
		String bodyWithoutClassification = """
				{"ratedPumpFlowGpm":500.0}""";

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithoutClassification))
				.andExpect(status().isBadRequest());
	}

	private FireWaterStorageResponse postForResponse(FireWaterStorageRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), FireWaterStorageResponse.class);
	}

}
