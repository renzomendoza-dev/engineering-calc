package com.renzoproject.calc_api.mechanical.pump;

import com.renzoproject.calc_api.mechanical.pipe.DiameterSpecTypeDto;
import com.renzoproject.calc_api.mechanical.pipe.FrictionFactorMethodDto;
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
class PumpTDHControllerTest {

	private static final String URL = "/api/mechanical/pump/tdh";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static PipeSegmentSpecDto giSegment(String nominalLabel, double lengthM) {
		return new PipeSegmentSpecDto(DiameterSpecTypeDto.NOMINAL, "GI", "SCH40", nominalLabel, null, null, lengthM, FrictionFactorMethodDto.SWAMEE_JAIN);
	}

	/** totalDynamicHead must always equal the sum of the response's own component fields. */
	private static void assertTotalDynamicHeadIsSumOfComponents(PumpTDHResponse response) {
		double expectedSum = response.staticHeadMeters() + response.totalSuctionHeadLossMeters()
				+ response.totalDischargeHeadLossMeters() + response.residualPressureHeadMeters()
				+ response.velocityHeadMeters();
		assertEquals(expectedSum, response.totalDynamicHeadMeters(), DELTA);
	}

	@Test
	void flooded_withSuctionAndDischargeSegments_returns200() throws Exception {
		PumpTDHRequest request = new PumpTDHRequest(
				"WATER", 20.0, 5.0, "L/s",
				SuctionConditionDto.FLOODED, null, 2.0, List.of(giSegment("2", 10.0)),
				15.0, 0.0, List.of(giSegment("2", 20.0)),
				false);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		PumpTDHResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), PumpTDHResponse.class);

		// staticHead = dischargeElevation(15) - suctionHeadFlooded(2) -- pure arithmetic, no
		// friction math involved, so this is exactly verifiable without duplicating Colebrook
		// numbers already covered by PipePressureLossControllerTest.
		assertEquals(13.0, response.staticHeadMeters(), DELTA);
		assertEquals(1, response.suctionSegmentDetails().size());
		assertEquals(1, response.dischargeSegmentDetails().size());
		assertTrue(response.totalSuctionHeadLossMeters() > 0);
		assertTrue(response.totalDischargeHeadLossMeters() > 0);
		assertTotalDynamicHeadIsSumOfComponents(response);
	}

	@Test
	void lift_withEmptySuctionSegments_returns200() throws Exception {
		PumpTDHRequest request = new PumpTDHRequest(
				"WATER", 20.0, 5.0, "L/s",
				SuctionConditionDto.LIFT, 3.0, null, List.of(),
				10.0, 0.0, List.of(giSegment("2", 15.0)),
				false);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		PumpTDHResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), PumpTDHResponse.class);

		// staticHead = dischargeElevation(10) + suctionLift(3).
		assertEquals(13.0, response.staticHeadMeters(), DELTA);
		assertEquals(0.0, response.totalSuctionHeadLossMeters(), DELTA);
		assertTrue(response.suctionSegmentDetails().isEmpty());
		assertEquals(1, response.dischargeSegmentDetails().size());
		assertTotalDynamicHeadIsSumOfComponents(response);
	}

	@Test
	void floodedMissingStaticSuctionHeadFlooded_returns400() throws Exception {
		PumpTDHRequest request = new PumpTDHRequest(
				"WATER", 20.0, 5.0, "L/s",
				SuctionConditionDto.FLOODED, null, null, List.of(),
				15.0, 0.0, List.of(),
				false);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unresolvableNominalSizeInSegment_returns400() throws Exception {
		PumpTDHRequest request = new PumpTDHRequest(
				"WATER", 20.0, 5.0, "L/s",
				SuctionConditionDto.FLOODED, null, 2.0, List.of(giSegment("999", 10.0)),
				15.0, 0.0, List.of(),
				false);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void negativeTotalDynamicHead_returns200WithStaticallyFedWarningTrue() throws Exception {
		// staticHead = dischargeElevation(1) - suctionHeadFlooded(100) = -99, no segments, no
		// residual pressure -> totalDynamicHead <= 0, but this must NOT error.
		PumpTDHRequest request = new PumpTDHRequest(
				"WATER", 20.0, 5.0, "L/s",
				SuctionConditionDto.FLOODED, null, 100.0, List.of(),
				1.0, 0.0, List.of(),
				false);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		PumpTDHResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), PumpTDHResponse.class);

		assertEquals(-99.0, response.totalDynamicHeadMeters(), DELTA);
		assertTrue(response.staticallyFedWarning());
	}

}
