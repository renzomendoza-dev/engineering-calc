package com.renzoproject.calc_api.electrical.conduitfill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConduitFillControllerTest {

	private static final String URL = "/api/electrical/conduit-fill";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void validRequest_returns200WithHandVerifiedSizeAndFillPercent() throws Exception {
		// THHN "100" area = 208.7mm2 x3 = 626.1mm2. 3 conductors -> 40% rule, over2Wires40 column.
		// EMT over2Wires40: ...40->526, 50->866 (first >= 626.1) -> recommended size 50mm.
		// actualFillPercent ~= 28.92%: over the 25% practical pull-ease threshold, but still
		// within the legal 40% limit -> practicalFillAdvisory should flag mayBeDifficultToPull.
		ConduitFillRequest request = new ConduitFillRequest(
				List.of(new ConductorFillEntryDto("THHN", "100", 3)), "EMT");

		double expectedArea = 208.7 * 3;
		double expectedFillPercent = (expectedArea / 2165.0) * 100.0;

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		ConduitFillResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), ConduitFillResponse.class);

		assertEquals("50", response.recommendedTradeSizeMm());
		assertEquals(expectedArea, response.totalConductorAreaMm2(), DELTA);
		assertEquals(3, response.totalConductorCount());
		assertEquals(40.0, response.allowedFillPercent(), DELTA);
		assertEquals(expectedFillPercent, response.actualFillPercentAtRecommendedSize(), DELTA);
		assertFalse(response.requiresMultipleConduits());
		assertTrue(response.actualFillPercentAtRecommendedSize() <= response.allowedFillPercent());
		assertTrue(response.practicalFillAdvisory().mayBeDifficultToPull());
		assertNotNull(response.practicalFillAdvisory().note());
		assertFalse(response.practicalFillAdvisory().note().isEmpty());
	}

	@Test
	void fillWellUnder25Percent_practicalAdvisoryNotDifficult() throws Exception {
		// TF "1.25" area = 7.1mm2 x4 = 28.4mm2. 4 conductors -> 40% rule, over2Wires40 column.
		// PVC_SCHEDULE_40_HDPE 15mm: over2Wires40=74 (28.4 fits), totalArea100=184.
		// actualFillPercent ~= 15.43%, well under the 25% practical threshold.
		ConduitFillRequest request = new ConduitFillRequest(
				List.of(new ConductorFillEntryDto("TF", "1.25", 4)), "PVC_SCHEDULE_40_HDPE");

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		ConduitFillResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), ConduitFillResponse.class);

		assertEquals("15", response.recommendedTradeSizeMm());
		assertTrue(response.actualFillPercentAtRecommendedSize() < 25.0);
		assertFalse(response.practicalFillAdvisory().mayBeDifficultToPull());
		assertNull(response.practicalFillAdvisory().note());
	}

	@Test
	void oversizedLoad_returns200WithRequiresMultipleConduitsTrue() throws Exception {
		// 50 x THHN "500" (870.9mm2 each) in FMC — far beyond FMC's largest entry. Not an error.
		ConduitFillRequest request = new ConduitFillRequest(
				List.of(new ConductorFillEntryDto("THHN", "500", 50)), "FMC");

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		ConduitFillResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), ConduitFillResponse.class);

		assertNull(response.recommendedTradeSizeMm());
		assertNull(response.actualFillPercentAtRecommendedSize());
		assertTrue(response.requiresMultipleConduits());
		assertNull(response.practicalFillAdvisory());
	}

	@Test
	void unknownInsulationType_returns400() throws Exception {
		ConduitFillRequest request = new ConduitFillRequest(
				List.of(new ConductorFillEntryDto("NOT_REAL", "100", 1)), "EMT");

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknownConduitType_returns400() throws Exception {
		ConduitFillRequest request = new ConduitFillRequest(
				List.of(new ConductorFillEntryDto("THHN", "100", 1)), "NOT_REAL");

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void emptyConductorsList_returns400() throws Exception {
		ConduitFillRequest request = new ConduitFillRequest(List.of(), "EMT");

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void zeroQuantity_returns400() throws Exception {
		ConduitFillRequest request = new ConduitFillRequest(
				List.of(new ConductorFillEntryDto("THHN", "100", 0)), "EMT");

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

}
