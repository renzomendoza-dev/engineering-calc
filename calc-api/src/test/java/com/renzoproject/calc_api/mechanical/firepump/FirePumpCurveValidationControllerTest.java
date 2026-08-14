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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FirePumpCurveValidationControllerTest {

	private static final String URL = "/api/mechanical/firepump/curve-validation";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void compliantCurve_returns200WithAllChecksPassing() throws Exception {
		// rated 1000 GPM @ 100 psi; NFPA 20 published percentages: churn<=140%, overload>=65% at 150%.
		// churn @0=135psi (<=140), overload @1500=70psi (>=65) -> all compliant.
		FirePumpCurveValidationRequest request = new FirePumpCurveValidationRequest(
				1000.0, 100.0,
				List.of(
						new PumpCurvePointDto(0.0, 135.0),
						new PumpCurvePointDto(500.0, 115.0),
						new PumpCurvePointDto(1000.0, 100.0),
						new PumpCurvePointDto(1500.0, 70.0)),
				1000.0, 100.0);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		FirePumpCurveValidationResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), FirePumpCurveValidationResponse.class);

		assertTrue(response.meetsCapacityDemand());
		assertEquals(135.0, response.churnPressurePsi(), DELTA);
		assertTrue(response.churnCompliant());
		assertEquals(70.0, response.overloadPressurePsi(), DELTA);
		assertTrue(response.overloadCompliant());
		assertTrue(response.overallCompliant());
	}

	@Test
	void emptyCurvePoints_returns400() throws Exception {
		FirePumpCurveValidationRequest request = new FirePumpCurveValidationRequest(
				1000.0, 100.0, List.of(), 1000.0, 100.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void curveDoesNotBracket150PercentFlow_returns400() throws Exception {
		// Curve only spans to 1200 GPM, but 150% of rated (1000) is 1500 GPM.
		FirePumpCurveValidationRequest request = new FirePumpCurveValidationRequest(
				1000.0, 100.0,
				List.of(
						new PumpCurvePointDto(0.0, 135.0),
						new PumpCurvePointDto(1000.0, 100.0),
						new PumpCurvePointDto(1200.0, 90.0)),
				1000.0, 100.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

}
