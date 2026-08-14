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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PipeVelocityControllerTest {

	private static final String URL = "/api/mechanical/pipe-velocity";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void velocityFromDiameter_nominalSpec_returns200WithCalculatedVelocity() throws Exception {
		// GI SCH40 "2" -> internal diameter 52.48mm (published). Q=2 L/s = 0.002 m3/s.
		// V = Q / ((pi/4)*D^2) = 0.9245969608160564 m/s (precomputed).
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.VELOCITY_FROM_DIAMETER, 2.0, "L/s",
				DiameterSpecTypeDto.NOMINAL, "GI", "SCH40", "2", null, null,
				null, null, null, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		PipeVelocityResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), PipeVelocityResponse.class);

		assertEquals(PipeSizingModeDto.VELOCITY_FROM_DIAMETER, response.mode());
		assertEquals(0.9245969608160564, response.velocityValue(), DELTA);
		assertEquals("m/s", response.velocityUnit());
		assertNull(response.nominalPipeSize());
	}

	@Test
	void velocityFromDiameter_rawSpec_returns200WithCalculatedVelocity() throws Exception {
		// D=50mm, Q=0.002 m3/s -> V = 1.0185916357881302 m/s (precomputed).
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.VELOCITY_FROM_DIAMETER, 0.002, "m3/s",
				DiameterSpecTypeDto.RAW, null, null, null, 50.0, "mm",
				null, null, null, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		PipeVelocityResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), PipeVelocityResponse.class);

		assertEquals(PipeSizingModeDto.VELOCITY_FROM_DIAMETER, response.mode());
		assertEquals(1.0185916357881302, response.velocityValue(), DELTA);
	}

	@Test
	void diameterFromVelocity_returns200WithResolvedNominalSizeAndActualVelocity() throws Exception {
		// Q=0.01 m3/s, target=1.5 m/s -> Dmin=92.13177319235613mm, bracketed by GI SCH40 "3"
		// (77.92mm, too small) and "4" (102.26mm, chosen). Actual velocity at 102.26mm =
		// 1.2175829047940205 m/s (precomputed), which must be <= 1.5.
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.DIAMETER_FROM_VELOCITY, 0.01, "m3/s",
				null, null, null, null, null, null,
				1.5, "m/s", "GI", "SCH40");

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		PipeVelocityResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), PipeVelocityResponse.class);

		assertEquals(PipeSizingModeDto.DIAMETER_FROM_VELOCITY, response.mode());
		assertNull(response.velocityValue());
		assertEquals(92.13177319235613, response.calculatedMinDiameterValue(), 1e-6);
		assertEquals("4\" (DN100)", response.nominalPipeSize());
		assertEquals(102.26, response.actualInternalDiameterValue(), DELTA);
		assertEquals(1.2175829047940205, response.actualVelocityValue(), DELTA);
		assertTrue(response.actualVelocityValue() <= 1.5);
	}

	@Test
	void velocityFromDiameter_nominalSpecMissingNominalLabel_returns400() throws Exception {
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.VELOCITY_FROM_DIAMETER, 2.0, "L/s",
				DiameterSpecTypeDto.NOMINAL, "GI", "SCH40", null, null, null,
				null, null, null, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void diameterFromVelocity_missingPipeMaterial_returns400() throws Exception {
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.DIAMETER_FROM_VELOCITY, 0.01, "m3/s",
				null, null, null, null, null, null,
				1.5, "m/s", null, "SCH40");

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void velocityFromDiameter_unresolvableNominalSize_returns400() throws Exception {
		PipeVelocityRequest request = new PipeVelocityRequest(
				PipeSizingModeDto.VELOCITY_FROM_DIAMETER, 2.0, "L/s",
				DiameterSpecTypeDto.NOMINAL, "GI", "SCH40", "999", null, null,
				null, null, null, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andReturn();

		String body = mvcResult.getResponse().getContentAsString();
		assertTrue(body.contains("No pipe dimension data"));
	}

}
