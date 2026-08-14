package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc_api.electrical.wiresizing.VoltageDropCheckRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MotorConductorSizingControllerTest {

	private static final String URL = "/api/electrical/motor-conductor-sizing";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void straightforwardCase_requiredAmpacityIsExactlyFlcTimes1point25() throws Exception {
		// THREE_PHASE INDUCTION "10" HP @ 230V = 28A published FLC.
		MotorConductorSizingRequest request = new MotorConductorSizingRequest(
				"THREE_PHASE", "INDUCTION", "10", 230, null,
				30.0, 2, "THHN", "COPPER", 75, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		MotorConductorSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), MotorConductorSizingResponse.class);

		assertEquals(28.0, response.motorFlcResult().flcAmps(), DELTA);
		// The critical assertion: requiredAmpacityAmps must be exactly flcAmps * 1.25, proving
		// the 125% factor was applied exactly once (via isContinuousLoad), not zero or two
		// times — verified here through the full HTTP round trip, same as the calc-core test.
		assertEquals(response.motorFlcResult().flcAmps() * 1.25, response.wireSizingResult().requiredAmpacityAmps(), DELTA);
		assertEquals(35.0, response.wireSizingResult().requiredAmpacityAmps(), DELTA);
	}

	@Test
	void synchronousMotorWithPowerFactor_adjustedFlcFlowsThroughAsLoadCurrent() throws Exception {
		// THREE_PHASE SYNCHRONOUS "25" HP @ 230V = 53A base (unity PF), 90% PF multiplier = 1.1
		// -> adjusted flcAmps = 58.3A. requiredAmpacityAmps must be based on the ADJUSTED value.
		MotorConductorSizingRequest request = new MotorConductorSizingRequest(
				"THREE_PHASE", "SYNCHRONOUS", "25", 230, 90,
				30.0, 2, "THHN", "COPPER", 75, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		MotorConductorSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), MotorConductorSizingResponse.class);

		assertEquals(53.0, response.motorFlcResult().baseFlcAmps(), DELTA);
		assertEquals(58.3, response.motorFlcResult().flcAmps(), DELTA);
		assertEquals(58.3 * 1.25, response.wireSizingResult().requiredAmpacityAmps(), DELTA);
	}

	@Test
	void voltageDropCheckProvided_wireSizingResultIncludesIt() throws Exception {
		VoltageDropCheckRequestDto voltageDropCheck = new VoltageDropCheckRequestDto(
				"THREE_PHASE_AC", 10.0, 0.9, 230.0, "PVC", 1);
		MotorConductorSizingRequest request = new MotorConductorSizingRequest(
				"THREE_PHASE", "INDUCTION", "10", 230, null,
				30.0, 2, "THHN", "COPPER", 75, voltageDropCheck);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		MotorConductorSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), MotorConductorSizingResponse.class);

		assertNotNull(response.wireSizingResult().voltageDropCheckResult());
		assertEquals(response.wireSizingResult().recommendedSizeLabel(),
				response.wireSizingResult().voltageDropCheckResult().sizeLabelChecked());
	}

	@Test
	void voltageDropCheckNotProvided_wireSizingResultOmitsIt() throws Exception {
		MotorConductorSizingRequest request = new MotorConductorSizingRequest(
				"THREE_PHASE", "INDUCTION", "10", 230, null,
				30.0, 2, "THHN", "COPPER", 75, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		MotorConductorSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), MotorConductorSizingResponse.class);

		assertNull(response.wireSizingResult().voltageDropCheckResult());
	}

	@Test
	void invalidMotorInput_threePhaseWithNullMotorClass_returns400() throws Exception {
		MotorConductorSizingRequest request = new MotorConductorSizingRequest(
				"THREE_PHASE", null, "10", 230, null,
				30.0, 2, "THHN", "COPPER", 75, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

}
