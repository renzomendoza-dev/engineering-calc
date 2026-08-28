package com.renzoproject.calc_api.electrical.powerconsumption;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PowerConsumptionControllerTest {

	private static final String URL = "/api/electrical/power-consumption";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static PowerConsumptionRequest directWattage(double wattageInput, double totalOperatingHours, Double pricePerKwh) {
		return new PowerConsumptionRequest(
				"DIRECT_WATTAGE", wattageInput, null, null, null, null, null, null, totalOperatingHours, pricePerKwh);
	}

	private static PowerConsumptionRequest voltageCurrent(
			String circuitType, double voltage, double currentAmps, Double powerFactor, double totalOperatingHours) {
		return new PowerConsumptionRequest(
				"VOLTAGE_CURRENT", null, circuitType, voltage, currentAmps, powerFactor, null, null, totalOperatingHours, null);
	}

	private static PowerConsumptionRequest horsepower(double horsepowerInput, Double motorEfficiencyPercent, double totalOperatingHours) {
		return new PowerConsumptionRequest(
				"HORSEPOWER", null, null, null, null, null, horsepowerInput, motorEfficiencyPercent, totalOperatingHours, null);
	}

	@Test
	void directWattage_validRequest_returns200_matchesHandCalculatedValue() throws Exception {
		// 65W charger, 5 hours -> 65/1000 * 5 = 0.325 kWh
		PowerConsumptionResponse response = postForResponse(directWattage(65.0, 5.0, null));

		assertEquals(65.0, response.powerWatts(), DELTA);
		assertEquals(0.325, response.energyConsumedKwh(), DELTA);
	}

	@Test
	void voltageCurrent_singlePhaseAc_explicitPowerFactor_returns200() throws Exception {
		// P = V * I * PF = 230 * 10 * 0.9 = 2070W
		PowerConsumptionResponse response = postForResponse(voltageCurrent("SINGLE_PHASE_AC", 230.0, 10.0, 0.9, 2.0));

		assertEquals(2070.0, response.powerWatts(), DELTA);
	}

	@Test
	void voltageCurrent_singlePhaseAc_noPowerFactor_defaultsToOne_returns200() throws Exception {
		// PF omitted -> defaults to 1.0, P = V * I = 230 * 10 = 2300W
		PowerConsumptionResponse response = postForResponse(voltageCurrent("SINGLE_PHASE_AC", 230.0, 10.0, null, 2.0));

		assertEquals(2300.0, response.powerWatts(), DELTA);
	}

	@Test
	void voltageCurrent_threePhaseAc_returns200() throws Exception {
		// P = sqrt(3) * V * I * PF
		PowerConsumptionResponse response = postForResponse(voltageCurrent("THREE_PHASE_AC", 400.0, 20.0, 0.85, 2.0));

		double expected = Math.sqrt(3) * 400.0 * 20.0 * 0.85;
		assertEquals(expected, response.powerWatts(), DELTA);
	}

	@Test
	void horsepower_explicitEfficiency_isEstimatedPowerTrue_returns200() throws Exception {
		PowerConsumptionResponse response = postForResponse(horsepower(2.0, 90.0, 3.0));

		double expected = (2.0 * 746.0) / (90.0 / 100.0);
		assertEquals(expected, response.powerWatts(), DELTA);
		assertTrue(response.isEstimatedPower());
	}

	@Test
	void horsepower_noEfficiency_defaultsTo85Percent_isEstimatedPowerTrue_returns200() throws Exception {
		PowerConsumptionResponse response = postForResponse(horsepower(1.0, null, 3.0));

		double expected = (1.0 * 746.0) / (85.0 / 100.0);
		assertEquals(expected, response.powerWatts(), DELTA);
		assertTrue(response.isEstimatedPower());
	}

	@Test
	void withPricePerKwh_estimatedCostPopulated_returns200() throws Exception {
		PowerConsumptionResponse response = postForResponse(directWattage(1000.0, 10.0, 0.15));

		assertEquals(10.0, response.energyConsumedKwh(), DELTA);
		assertEquals(1.5, response.estimatedCost(), DELTA);
	}

	@Test
	void withoutPricePerKwh_estimatedCostIsNull_returns200() throws Exception {
		PowerConsumptionResponse response = postForResponse(directWattage(1000.0, 10.0, null));

		assertNull(response.estimatedCost());
	}

	@Test
	void invalidModeString_returns400() throws Exception {
		PowerConsumptionRequest request = new PowerConsumptionRequest(
				"NOT_A_REAL_MODE", 65.0, null, null, null, null, null, null, 5.0, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void directWattage_withVoltageAndCurrentAmps_isCalcCoreValidation_returns400() throws Exception {
		PowerConsumptionRequest request = new PowerConsumptionRequest(
				"DIRECT_WATTAGE", 65.0, null, 120.0, 5.0, null, null, null, 5.0, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void totalOperatingHoursZero_isBeanValidation_returns400() throws Exception {
		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(directWattage(65.0, 0.0, null))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void totalOperatingHoursNegative_isBeanValidation_returns400() throws Exception {
		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(directWattage(65.0, -1.0, null))))
				.andExpect(status().isBadRequest());
	}

	private PowerConsumptionResponse postForResponse(PowerConsumptionRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), PowerConsumptionResponse.class);
	}

}
