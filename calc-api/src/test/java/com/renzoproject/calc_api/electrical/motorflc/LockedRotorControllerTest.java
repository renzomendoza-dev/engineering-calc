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
class LockedRotorControllerTest {

	private static final String URL = "/api/electrical/locked-rotor";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void singlePhaseCase_matchesHandCheckedValue() throws Exception {
		// Single-phase "5" HP @ 230V = 168A locked-rotor (published).
		LockedRotorRequest request = new LockedRotorRequest(false, "5", 230);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		LockedRotorResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), LockedRotorResponse.class);

		assertEquals(168.0, response.lockedRotorAmps(), DELTA);
	}

	@Test
	void polyphaseCase_matchesHandCheckedValue() throws Exception {
		// Design B/C/D polyphase "5" HP @ 230V = 92A locked-rotor (published).
		LockedRotorRequest request = new LockedRotorRequest(true, "5", 230);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		LockedRotorResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), LockedRotorResponse.class);

		assertEquals(92.0, response.lockedRotorAmps(), DELTA);
	}

	@Test
	void unknownCombination_returns400() throws Exception {
		LockedRotorRequest request = new LockedRotorRequest(false, "9999", 230);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

}
