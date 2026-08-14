package com.renzoproject.calc_api.mechanical.firepump;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FirePumpPowerControllerTest {

	private static final String URL = "/api/mechanical/firepump/power";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void ratedPointOnly_returns200WithSinglePointAndResolvedMotorSize() throws Exception {
		// headFeet=100*2.31=231; whp=(500*231*1)/3960=29.166666666666668; bhp=whp/0.75=38.88888888888889.
		// Published motor HP steps include ...30, 40... -> rounds up to 40.
		FirePumpPowerRequest request = new FirePumpPowerRequest(
				FirePumpPowerInputTypeDto.RATED_POINT_ONLY,
				500.0, 100.0,
				null, null, null,
				0.75, 1.0);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		FirePumpPowerResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), FirePumpPowerResponse.class);

		assertEquals(1, response.evaluatedPoints().size());
		assertEquals(38.88888888888889, response.maxBrakeHorsepower(), DELTA);
		assertEquals("40 HP", response.recommendedElectricMotorSize());
		assertTrue(response.driverSizingNote().contains("rated-point-only"));
	}

	@Test
	void fullCurve_returns200WithTwoPointsAndHigherBhpChosen() throws Exception {
		// rated 500 GPM @ 100psi -> bhp=38.88888888888889. 150% flow=750 GPM=75psi (exact curve
		// point) -> bhp=43.75, which is higher -> maxBrakeHorsepower=43.75 -> rounds up to 50 HP.
		FirePumpPowerRequest request = new FirePumpPowerRequest(
				FirePumpPowerInputTypeDto.FULL_CURVE,
				null, null,
				500.0, 100.0,
				List.of(
						new PumpCurvePointDto(0.0, 135.0),
						new PumpCurvePointDto(250.0, 120.0),
						new PumpCurvePointDto(500.0, 100.0),
						new PumpCurvePointDto(750.0, 75.0),
						new PumpCurvePointDto(900.0, 50.0)),
				0.75, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		FirePumpPowerResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), FirePumpPowerResponse.class);

		assertEquals(2, response.evaluatedPoints().size());
		assertEquals(43.75, response.maxBrakeHorsepower(), DELTA);
		assertEquals("50 HP", response.recommendedElectricMotorSize());
		assertNull(response.driverSizingNote());
	}

	@Test
	void ratedPointOnly_missingFlowAndPressure_returns400() throws Exception {
		FirePumpPowerRequest request = new FirePumpPowerRequest(
				FirePumpPowerInputTypeDto.RATED_POINT_ONLY,
				null, null,
				null, null, null,
				0.75, 1.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void fullCurve_missingCurvePoints_returns400() throws Exception {
		FirePumpPowerRequest request = new FirePumpPowerRequest(
				FirePumpPowerInputTypeDto.FULL_CURVE,
				null, null,
				500.0, 100.0, null,
				0.75, 1.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void brakeHorsepowerExceedsLargestMotorStep_returns400() throws Exception {
		// Very high flow/pressure -> bhp far exceeds the largest published motor step (1000 HP).
		FirePumpPowerRequest request = new FirePumpPowerRequest(
				FirePumpPowerInputTypeDto.RATED_POINT_ONLY,
				20000.0, 300.0,
				null, null, null,
				0.75, 1.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

}
