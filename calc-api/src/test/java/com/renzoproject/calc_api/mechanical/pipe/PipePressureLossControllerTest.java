package com.renzoproject.calc_api.mechanical.pipe;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Note on coverage: a "non-convergent Colebrook-White iteration" 400 case is intentionally NOT
 * included here. Verified via direct experimentation (see PR/session notes) that the fixed-point
 * iteration only fails to converge at pathological relative-roughness values (roughness
 * approaching or exceeding ~3.7x the diameter) — physically nonsensical, and unreachable through
 * any real {@code reference/pipes/} nominal size (every published pipe's internal diameter is
 * many multiples of its published roughness). Constructing that failure would require either a
 * fake resolver (this controller test hits the real Spring-wired service, no mocking framework
 * used anywhere else in this codebase's controller tests) or an unrealistic RawDiameter input
 * that turbulent flow already rejects for an unrelated reason (no material for roughness
 * lookup). The other two calc-core CalculationException cases are both genuinely reachable and
 * covered below.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PipePressureLossControllerTest {

	private static final String URL = "/api/mechanical/pipe-pressure-loss";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void validRequest_swameeJain_returns200WithCalculatedValues() throws Exception {
		// GI SCH40 "2" -> ID=52.48mm (published). Water @ 20C -> density=998.2, viscosity=0.001002
		// (published). Q=5 L/s = 0.005 m3/s, L=100m.
		// V=2.3114924020401406, Re=120847.07429221581, relRoughness=0.002858231707317073.
		// Swamee-Jain f=0.02716600691185787 (precomputed independently via the same formula).
		PipePressureLossRequest request = new PipePressureLossRequest(
				"WATER", 20.0, 5.0, "L/s",
				DiameterSpecTypeDto.NOMINAL, "GI", "SCH40", "2", null, null,
				100.0, FrictionFactorMethodDto.SWAMEE_JAIN);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		PipePressureLossResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), PipePressureLossResponse.class);

		assertEquals(2.3114924020401406, response.velocityMetersPerSecond(), DELTA);
		assertEquals(120847.07429221581, response.reynoldsNumber(), 1e-3);
		assertEquals(FlowRegimeDto.TURBULENT, response.flowRegime());
		assertFalse(response.transitionalRegimeWarning());
		assertEquals(0.02716600691185787, response.frictionFactor(), 1e-9);
		assertEquals(14.101530456015132, response.headLossMeters(), 1e-3);
		assertEquals(138039.85385391713, response.pressureLossPascals(), 1e-1);
	}

	@Test
	void validRequest_colebrookWhite_returns200WithConvergedValues() throws Exception {
		// Same scenario as the Swamee-Jain test. Colebrook f=0.026941177656257295 (precomputed
		// via the standard x=1/sqrt(f) fixed-point iteration, converging in 4 iterations).
		PipePressureLossRequest request = new PipePressureLossRequest(
				"WATER", 20.0, 5.0, "L/s",
				DiameterSpecTypeDto.NOMINAL, "GI", "SCH40", "2", null, null,
				100.0, FrictionFactorMethodDto.COLEBROOK_WHITE);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		PipePressureLossResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), PipePressureLossResponse.class);

		assertEquals(0.026941177656257295, response.frictionFactor(), 1e-9);
		assertEquals(13.984824434201125, response.headLossMeters(), 1e-3);
		assertEquals(136897.4188362907, response.pressureLossPascals(), 1e-1);
	}

	@Test
	void nominalDiameterSpecMissingNominalLabel_returns400() throws Exception {
		PipePressureLossRequest request = new PipePressureLossRequest(
				"WATER", 20.0, 5.0, "L/s",
				DiameterSpecTypeDto.NOMINAL, "GI", "SCH40", null, null, null,
				100.0, FrictionFactorMethodDto.SWAMEE_JAIN);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rawDiameterSpecMissingRawDiameterUnit_returns400() throws Exception {
		PipePressureLossRequest request = new PipePressureLossRequest(
				"WATER", 20.0, 5.0, "L/s",
				DiameterSpecTypeDto.RAW, null, null, null, 52.48, null,
				100.0, FrictionFactorMethodDto.SWAMEE_JAIN);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void fluidTemperatureOutOfTableRange_returns400() throws Exception {
		// Published water.json table spans 0-100 degC.
		PipePressureLossRequest request = new PipePressureLossRequest(
				"WATER", 150.0, 5.0, "L/s",
				DiameterSpecTypeDto.NOMINAL, "GI", "SCH40", "2", null, null,
				100.0, FrictionFactorMethodDto.SWAMEE_JAIN);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unresolvableNominalSize_returns400() throws Exception {
		PipePressureLossRequest request = new PipePressureLossRequest(
				"WATER", 20.0, 5.0, "L/s",
				DiameterSpecTypeDto.NOMINAL, "GI", "SCH40", "999", null, null,
				100.0, FrictionFactorMethodDto.SWAMEE_JAIN);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

}
