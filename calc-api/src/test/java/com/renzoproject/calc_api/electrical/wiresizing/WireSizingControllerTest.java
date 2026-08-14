package com.renzoproject.calc_api.electrical.wiresizing;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WireSizingControllerTest {

	private static final String URL = "/api/electrical/wire-sizing";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void validRequestWithoutVoltageDropCheck_matchesHandVerifiedRecommendation() throws Exception {
		// THHN copper -> 90C column. 30C ambient, 90C factor = 1.0. count=2 -> no adjustment.
		// COPPER "2.0" @ 90C = 25A >= 15A required -> recommended immediately.
		WireSizingRequest request = new WireSizingRequest(
				15.0, false, 30.0, 2, "THHN", "COPPER", 75, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		WireSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), WireSizingResponse.class);

		assertEquals("2.0", response.recommendedSizeLabel());
		assertEquals(25.0, response.baseAmpacityAmps(), DELTA);
		assertEquals(1.0, response.tempCorrectionFactor(), DELTA);
		assertEquals(1.0, response.adjustmentFactor(), DELTA);
		assertEquals(25.0, response.deratedAmpacityAmps(), DELTA);
		assertEquals(15.0, response.requiredAmpacityAmps(), DELTA);
		assertNull(response.voltageDropCheckResult());
	}

	@Test
	void voltageDropCheck_ampacityBasedSizeAlsoPasses_upsizedRecommendationNull() throws Exception {
		WireSizingRequest request = new WireSizingRequest(
				15.0, false, 30.0, 2, "THHN", "COPPER", 75,
				new VoltageDropCheckRequestDto("SINGLE_PHASE_AC", 5.0, 0.9, 230.0, "PVC", 1));

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		WireSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), WireSizingResponse.class);

		assertEquals("2.0", response.recommendedSizeLabel());
		assertNotNull(response.voltageDropCheckResult());
		assertEquals("2.0", response.voltageDropCheckResult().sizeLabelChecked());
		assertFalse(response.voltageDropCheckResult().exceedsRecommendedLimit());
		assertNull(response.voltageDropCheckResult().upsizedRecommendation());
	}

	@Test
	void voltageDropCheck_ampacityBasedSizeFails_upsizedRecommendationPopulated() throws Exception {
		// Same ampacity scenario, but a 200m run pushes voltage drop at "2.0" far past 3%.
		WireSizingRequest request = new WireSizingRequest(
				15.0, false, 30.0, 2, "THHN", "COPPER", 75,
				new VoltageDropCheckRequestDto("SINGLE_PHASE_AC", 200.0, 0.9, 230.0, "PVC", 1));

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		WireSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), WireSizingResponse.class);

		assertEquals("2.0", response.recommendedSizeLabel());
		assertNotNull(response.voltageDropCheckResult());
		assertTrue(response.voltageDropCheckResult().exceedsRecommendedLimit());
		assertNotNull(response.voltageDropCheckResult().upsizedRecommendation());
	}

	@Test
	void unknownInsulationType_returns400() throws Exception {
		WireSizingRequest request = new WireSizingRequest(
				15.0, false, 30.0, 2, "NOT_REAL", "COPPER", 75, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknownConductorMaterial_returns400() throws Exception {
		WireSizingRequest request = new WireSizingRequest(
				15.0, false, 30.0, 2, "THHN", "NOT_REAL", 75, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void noSizeSatisfiesRequiredAmpacity_returns400WithErrorMessage() throws Exception {
		// 10,000A is far beyond even the largest published size (500 COPPER @ 90C = 595A).
		WireSizingRequest request = new WireSizingRequest(
				10000.0, false, 30.0, 1, "THHN", "COPPER", 75, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andReturn();

		String body = mvcResult.getResponse().getContentAsString();
		assertTrue(body.contains("No conductor size in the table satisfies the required ampacity"));
	}

}
