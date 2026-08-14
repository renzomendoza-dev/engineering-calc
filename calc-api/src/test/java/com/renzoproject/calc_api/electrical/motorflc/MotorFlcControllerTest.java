package com.renzoproject.calc_api.electrical.motorflc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MotorFlcControllerTest {

	private static final String URL = "/api/electrical/motor-flc";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void threePhaseInductionMotor_matchesHandVerifiedValues() throws Exception {
		// THREE_PHASE INDUCTION "10" HP @ 230V = 28A (published). 28 * 1.25 = 35.0.
		MotorFlcRequest request = new MotorFlcRequest("THREE_PHASE", "INDUCTION", "10", 230, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		MotorFlcResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), MotorFlcResponse.class);

		assertEquals(28.0, response.flcAmps(), DELTA);
		assertEquals(28.0, response.baseFlcAmps(), DELTA);
		assertEquals(35.0, response.minimumConductorAmpacity(), DELTA);
	}

	@Test
	void synchronousMotorWithPowerFactor_appliesMultiplier() throws Exception {
		// THREE_PHASE SYNCHRONOUS "25" HP @ 230V = 53A base, 90% PF multiplier = 1.1 -> 58.3A.
		MotorFlcRequest request = new MotorFlcRequest("THREE_PHASE", "SYNCHRONOUS", "25", 230, 90);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		MotorFlcResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), MotorFlcResponse.class);

		assertEquals(53.0, response.baseFlcAmps(), DELTA);
		assertEquals(58.3, response.flcAmps(), DELTA);
	}

	@Test
	void threePhaseWithNullMotorClass_returns400() throws Exception {
		MotorFlcRequest request = new MotorFlcRequest("THREE_PHASE", null, "10", 230, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknownPhaseType_returns400() throws Exception {
		MotorFlcRequest request = new MotorFlcRequest("NOT_REAL", null, "10", 230, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unpublishedCombination_returns400() throws Exception {
		MotorFlcRequest request = new MotorFlcRequest("THREE_PHASE", "INDUCTION", "5000", 230, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

}
