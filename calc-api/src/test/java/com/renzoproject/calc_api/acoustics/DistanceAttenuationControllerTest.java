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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DistanceAttenuationControllerTest {

	private static final String URL = "/api/acoustics/distance-attenuation";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void doublingDistance_returns200MatchingCoreLayerSpotCheck() throws Exception {
		// 100 dB at 1m -> ~93.98 dB at 2m (100 + (-6.020599913279624)) -- the same sanity check
		// used at the calc-core layer, now verified through the full HTTP round trip.
		DistanceAttenuationRequest request = new DistanceAttenuationRequest(100.0, 1.0, 2.0);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		DistanceAttenuationResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), DistanceAttenuationResponse.class);

		assertEquals(-6.020599913279624, response.attenuationDb(), DELTA);
		assertEquals(93.97940008672038, response.targetSplDb(), DELTA);
	}

	@Test
	void movingCloser_returns200WithPositiveAttenuation() throws Exception {
		DistanceAttenuationRequest request = new DistanceAttenuationRequest(80.0, 4.0, 1.0);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		DistanceAttenuationResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), DistanceAttenuationResponse.class);

		assertTrue(response.attenuationDb() > 0);
		assertTrue(response.targetSplDb() > 80.0);
	}

	@Test
	void missingReferenceSplDb_returns400() throws Exception {
		DistanceAttenuationRequest request = new DistanceAttenuationRequest(null, 1.0, 2.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nonPositiveReferenceDistance_returns400() throws Exception {
		DistanceAttenuationRequest request = new DistanceAttenuationRequest(100.0, 0.0, 2.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nonPositiveTargetDistance_returns400() throws Exception {
		DistanceAttenuationRequest request = new DistanceAttenuationRequest(100.0, 1.0, -2.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

}
