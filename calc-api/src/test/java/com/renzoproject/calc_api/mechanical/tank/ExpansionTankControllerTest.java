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
class ExpansionTankControllerTest {

	private static final String URL = "/api/mechanical/tank/expansion";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static ExpansionTankRequest domesticWaterHeaterRequest(Double additionalVolumeFactor) {
		return new ExpansionTankRequest(
				HeatingSystemTypeDto.DOMESTIC_WATER_HEATER,
				190.0,
				10.0,
				20.0,
				additionalVolumeFactor,
				10.0,
				60.0,
				300.0,
				550.0,
				0.0);
	}

	private static ExpansionTankRequest hydronicHeatingRequest() {
		return new ExpansionTankRequest(
				HeatingSystemTypeDto.HYDRONIC_HEATING,
				500.0,
				50.0,
				25.0,
				1.0,
				10.0,
				90.0,
				150.0,
				300.0,
				0.0);
	}

	@Test
	void domesticWaterHeater_validRequest_returns200() throws Exception {
		ExpansionTankResponse response = postForResponse(domesticWaterHeaterRequest(1.0));

		assertNotNull(response.requiredTankVolumeLiters());
		assertTrue(response.requiredTankVolumeLiters() > 0);
		assertTrue(response.requiredTankVolumeLiters() > response.expansionVolumeLiters());
	}

	@Test
	void hydronicHeating_validRequest_returns200() throws Exception {
		ExpansionTankResponse response = postForResponse(hydronicHeatingRequest());

		assertNotNull(response.requiredTankVolumeLiters());
		assertTrue(response.requiredTankVolumeLiters() > 0);
	}

	@Test
	void additionalVolumeFactorOmitted_returns200() throws Exception {
		ExpansionTankResponse response = postForResponse(domesticWaterHeaterRequest(null));

		assertNotNull(response.requiredTankVolumeLiters());
		assertTrue(response.requiredTankVolumeLiters() > 0);
	}

	@Test
	void hotTemperatureAtOrBelowColdTemperature_isCalcCoreValidation_returns400() throws Exception {
		ExpansionTankRequest request = new ExpansionTankRequest(
				HeatingSystemTypeDto.DOMESTIC_WATER_HEATER,
				190.0,
				10.0,
				20.0,
				1.0,
				60.0,
				60.0,
				300.0,
				550.0,
				0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void maxOperatingPressureAtOrBelowFillPressure_isCalcCoreValidation_returns400() throws Exception {
		ExpansionTankRequest request = new ExpansionTankRequest(
				HeatingSystemTypeDto.DOMESTIC_WATER_HEATER,
				190.0,
				10.0,
				20.0,
				1.0,
				10.0,
				60.0,
				550.0,
				550.0,
				0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void outOfRangeWaterTemperature_isCalcCoreValidation_returns400() throws Exception {
		ExpansionTankRequest request = new ExpansionTankRequest(
				HeatingSystemTypeDto.DOMESTIC_WATER_HEATER,
				190.0,
				10.0,
				20.0,
				1.0,
				10.0,
				150.0,
				300.0,
				550.0,
				0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingRequiredFields_isBeanValidation_returns400() throws Exception {
		String json = "{\"systemType\":\"DOMESTIC_WATER_HEATER\"}";

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isBadRequest());
	}

	private ExpansionTankResponse postForResponse(ExpansionTankRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ExpansionTankResponse.class);
	}

}
