package com.renzoproject.calc_api.mechanical.firepump;

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
class FirePumpDemandControllerTest {

	private static final String URL = "/api/mechanical/firepump/demand";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void flooded_returns200WithRatedFlowAndPressure() throws Exception {
		FirePumpDemandRequest request = new FirePumpDemandRequest(
				500.0, 100.0, SuctionConditionDto.FLOODED, 20.0, null, 10.0);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		FirePumpDemandResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), FirePumpDemandResponse.class);

		assertEquals(550.0, response.ratedFlowGpm(), DELTA);
		assertEquals(80.0, response.ratedPressurePsi(), DELTA);
	}

	@Test
	void lift_returns200WithLiftEquivalentApplied() throws Exception {
		FirePumpDemandRequest request = new FirePumpDemandRequest(
				300.0, 90.0, SuctionConditionDto.LIFT, null, 10.0, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		FirePumpDemandResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), FirePumpDemandResponse.class);

		assertEquals(300.0, response.ratedFlowGpm(), DELTA);
		assertEquals(94.33, response.ratedPressurePsi(), DELTA);
	}

	@Test
	void flooded_missingAvailableSuctionPressure_returns400() throws Exception {
		FirePumpDemandRequest request = new FirePumpDemandRequest(
				500.0, 100.0, SuctionConditionDto.FLOODED, null, null, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void lift_missingSuctionLift_returns400() throws Exception {
		FirePumpDemandRequest request = new FirePumpDemandRequest(
				300.0, 90.0, SuctionConditionDto.LIFT, null, null, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void floodedWithSuctionExceedingDemandPressure_returns400() throws Exception {
		// availableSuctionPressurePsi (60) > requiredPressurePsiAtDemandPoint (50) -> ratedPressure <= 0.
		FirePumpDemandRequest request = new FirePumpDemandRequest(
				500.0, 50.0, SuctionConditionDto.FLOODED, 60.0, null, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

}
