package com.renzoproject.calc_api.mechanical.pump;

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
class PumpPowerControllerTest {

	private static final String URL = "/api/mechanical/pump/power";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void validRequest_returns200WithCorrectPowerAndResolvedMotorSize() throws Exception {
		// hydraulicPowerKw=12.2583125, shaftPowerKw=16.344416666666667 (precomputed).
		// Published motor kW steps include ...15, 18.5... -> rounds up to 18.5.
		PumpPowerRequest request = new PumpPowerRequest(0.05, "m3/s", 25.0, 0.75, 1000.0);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		PumpPowerResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), PumpPowerResponse.class);

		assertEquals(12.2583125, response.hydraulicPowerKw(), DELTA);
		assertEquals(16.344416666666667, response.shaftPowerKw(), DELTA);
		assertEquals("18.5 kW", response.recommendedMotorSizeKw());
	}

	@Test
	void nonPositiveTotalDynamicHead_returns400() throws Exception {
		PumpPowerRequest request = new PumpPowerRequest(0.05, "m3/s", 0.0, 0.75, 1000.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void efficiencyAboveOne_returns400ViaCoreValidation() throws Exception {
		// pumpEfficiency=1.5 passes @Positive at the DTO layer but calc-core's PumpPowerInput
		// rejects anything > 1.
		PumpPowerRequest request = new PumpPowerRequest(0.05, "m3/s", 25.0, 1.5, 1000.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

}
