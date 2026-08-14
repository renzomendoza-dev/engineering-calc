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
class FirePumpCapacityControllerTest {

	private static final String URL = "/api/mechanical/firepump/capacity";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void roundsUpToSmallestSufficientStandardCapacity_returns200() throws Exception {
		// Published capacities include ...300, 400... ; 320 GPM should round up to 400.
		FirePumpCapacityRequest request = new FirePumpCapacityRequest(320.0);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		FirePumpCapacityResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), FirePumpCapacityResponse.class);

		assertEquals(400.0, response.standardFlowGpm(), DELTA);
	}

	@Test
	void missingRatedFlow_returns400() throws Exception {
		FirePumpCapacityRequest request = new FirePumpCapacityRequest(null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void exceedsLargestStandardCapacity_returns400() throws Exception {
		// Largest published capacity is 5000 GPM.
		FirePumpCapacityRequest request = new FirePumpCapacityRequest(5001.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

}
