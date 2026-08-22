package com.renzoproject.calc_api.mechanical.tank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PressureTankControllerTest {

	private static final String URL = "/api/mechanical/tank/pressure";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static PressureTankRequest conventionalRequest() {
		return new PressureTankRequest(
				PumpControlTypeDto.CONVENTIONAL,
				40.0,
				1.0,
				140.0,
				280.0,
				0.0);
	}

	private static PressureTankRequest vfdRequest() {
		return new PressureTankRequest(
				PumpControlTypeDto.VFD,
				5.0,
				0.5,
				380.0,
				400.0,
				0.0);
	}

	@Test
	void conventional_validRequest_returns200() throws Exception {
		PressureTankResponse response = postForResponse(conventionalRequest());

		assertNotNull(response.requiredTankVolumeLiters());
		assertTrue(response.requiredTankVolumeLiters() > 0);
		assertTrue(response.requiredTankVolumeLiters() > response.drawdownVolumeLiters());
	}

	@Test
	void vfd_validRequest_returns200_andIsSmallerThanComparableConventionalCase() throws Exception {
		PressureTankResponse conventionalResponse = postForResponse(conventionalRequest());
		PressureTankResponse vfdResponse = postForResponse(vfdRequest());

		assertNotNull(vfdResponse.requiredTankVolumeLiters());
		assertTrue(vfdResponse.requiredTankVolumeLiters() > 0);
		assertTrue(vfdResponse.requiredTankVolumeLiters() < conventionalResponse.requiredTankVolumeLiters(),
				"VFD tank (" + vfdResponse.requiredTankVolumeLiters() + " L) should be smaller than the "
						+ "conventional tank (" + conventionalResponse.requiredTankVolumeLiters() + " L)");
	}

	@Test
	void highPressureAtOrBelowLowPressure_isCalcCoreValidation_returns400() throws Exception {
		PressureTankRequest request = new PressureTankRequest(
				PumpControlTypeDto.CONVENTIONAL, 40.0, 1.0, 280.0, 280.0, 0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nonPositiveFlowRate_isBeanValidation_returns400() throws Exception {
		PressureTankRequest request = new PressureTankRequest(
				PumpControlTypeDto.CONVENTIONAL, 0.0, 1.0, 140.0, 280.0, 0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nonPositiveRunTime_isBeanValidation_returns400() throws Exception {
		PressureTankRequest request = new PressureTankRequest(
				PumpControlTypeDto.CONVENTIONAL, 40.0, 0.0, 140.0, 280.0, 0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingRequiredFields_isBeanValidation_returns400() throws Exception {
		String json = "{\"controlType\":\"CONVENTIONAL\"}";

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest());
	}

	private PressureTankResponse postForResponse(PressureTankRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), PressureTankResponse.class);
	}

}
