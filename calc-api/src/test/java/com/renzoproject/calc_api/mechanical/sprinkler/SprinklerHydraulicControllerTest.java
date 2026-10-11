package com.renzoproject.calc_api.mechanical.sprinkler;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SprinklerHydraulicControllerTest {

	private static final String URL = "/api/mechanical/sprinkler/hydraulics";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static WaterSupplyDto supply() {
		return new WaterSupplyDto(80.0, 60.0, 1000.0);
	}

	private static SprinklerPipeSegmentDto segment(
			String from, String to, String nominalSize, double lengthFt, double elevationFt) {
		return new SprinklerPipeSegmentDto(from, to, "BI", "SCH40", nominalSize, 120.0, lengthFt, elevationFt, null);
	}

	/** The same one-sprinkler/one-branch/one-riser case hand-calculated in calc-core's test. */
	private static SprinklerHydraulicRequest handCalculatedRequest() {
		return new SprinklerHydraulicRequest(
				SprinklerSystemTypeDto.WET,
				SprinklerHazardDto.LIGHT,
				0.10,
				1500.0,
				100.0,
				"RISER",
				7.0,
				List.of(new RemoteAreaSprinklerDto("S1", 5.6, 130.0)),
				List.of(segment("S1", "B1", "1", 10.0, 0.0), segment("B1", "RISER", "2", 12.0, 10.0)),
				supply(),
				List.of(new DeviceLossDto("Alarm valve", 5.0)));
	}

	@Test
	void handCalculatedCase_returns200_withTheSameNumbersAsCalcCore() throws Exception {
		// Sprinkler K=5.6 at 7 psi = 14.8162 gpm; 1" branch loses 0.747662 psi over 10 ft;
		// 2" riser loses 0.0330269 psi over 12 ft plus 4.33 psi for the 10 ft rise; alarm valve
		// 5 psi -> 17.110689 psi required. Hose 100 gpm at the riser -> 114.8162 gpm demand, where
		// the 80/60@1000 supply gives 79.635216 psi. Margin 62.524527 psi (78.51%).
		SprinklerHydraulicResponse response = postForResponse(handCalculatedRequest());

		assertEquals(2, response.segments().size());
		assertEquals(14.8162073, response.segments().get(0).flowGpm(), 1e-6);
		assertEquals(0.747662, response.segments().get(0).frictionLossPsi(), 1e-6);
		assertEquals(5.502064, response.segments().get(0).velocityFps(), 1e-5);
		assertEquals(4.33, response.segments().get(1).elevationPressurePsi(), 1e-9);
		assertEquals(12.110689, response.segments().get(1).endPressurePsi(), 1e-6);

		assertEquals(114.8162073, response.totalDemandFlowGpm(), 1e-6);
		assertEquals(17.110689, response.requiredPressureAtSupplyPsi(), 1e-6);
		assertEquals(79.635216, response.availablePressureAtDemandPsi(), 1e-6);
		assertEquals(62.524527, response.safetyMarginPsi(), 1e-6);
		assertEquals(78.5137, response.safetyMarginPercent(), 1e-4);
		assertTrue(response.passes());
		assertTrue(response.warnings().isEmpty(), "expected no warnings, got " + response.warnings());
		assertTrue(response.supplyCurve().size() >= 5);
	}

	@Test
	void missingRequiredField_isBeanValidation_returns400() throws Exception {
		String json = "{\"systemType\":\"WET\"}";

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").isString())
				.andExpect(jsonPath("$.fieldErrors").isMap());
	}

	@Test
	void emptySprinklerList_isBeanValidation_returns400() throws Exception {
		SprinklerHydraulicRequest request = new SprinklerHydraulicRequest(
				SprinklerSystemTypeDto.WET, SprinklerHazardDto.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(),
				List.of(segment("S1", "RISER", "2", 10.0, 0.0)),
				supply(), null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.sprinklers").isString());
	}

	@Test
	void loopedNetwork_isCalcCoreValidation_returns400() throws Exception {
		SprinklerHydraulicRequest request = new SprinklerHydraulicRequest(
				SprinklerSystemTypeDto.WET, SprinklerHazardDto.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinklerDto("C1", 5.6, 130.0)),
				List.of(
						segment("A1", "A2", "1", 10.0, 0.0),
						segment("A2", "A1", "1", 10.0, 0.0),
						segment("C1", "RISER", "2", 10.0, 0.0)),
				supply(), null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("loop")))
				.andExpect(jsonPath("$.fieldErrors").isMap());
	}

	@Test
	void publishedFittingLength_isLookedUpAndAddedToTheSegment() throws Exception {
		// Table 27.2.3.1.1: a 90 degree standard elbow on 2" pipe is 5 ft, so two add 10 ft.
		SprinklerPipeSegmentDto withFitting = new SprinklerPipeSegmentDto(
				"S1", "RISER", "BI", "SCH40", "2", 120.0, 10.0, 0.0,
				List.of(new SegmentFittingDto("ELBOW_90_STANDARD", 2, null)));
		SprinklerHydraulicRequest request = new SprinklerHydraulicRequest(
				SprinklerSystemTypeDto.WET, SprinklerHazardDto.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinklerDto("S1", 5.6, 130.0)),
				List.of(withFitting), supply(), null);

		SprinklerHydraulicResponse response = postForResponse(request);

		assertEquals(10.0, response.segments().get(0).fittingEquivalentLengthFt(), 1e-9);
		assertEquals(20.0, response.segments().get(0).totalLengthFt(), 1e-9);
	}

	@Test
	void unpublishedFittingLength_isCalcCoreValidation_returns400() throws Exception {
		// Table 27.2.3.1.1 publishes no butterfly valve below 2", so a 1-1/2" one has no length
		// and must be refused rather than silently treated as zero.
		SprinklerPipeSegmentDto withFitting = new SprinklerPipeSegmentDto(
				"S1", "RISER", "BI", "SCH40", "1-1/2", 120.0, 10.0, 0.0,
				List.of(new SegmentFittingDto("BUTTERFLY_VALVE", 1, null)));
		SprinklerHydraulicRequest request = new SprinklerHydraulicRequest(
				SprinklerSystemTypeDto.WET, SprinklerHazardDto.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinklerDto("S1", 5.6, 130.0)),
				List.of(withFitting), supply(), null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("No published equivalent length")));
	}

	@Test
	void repeatedCalls_areSideEffectFree() throws Exception {
		// The frontend will call this on every debounced keystroke, so the same request must give
		// the same answer with no accumulated state in the shared resolvers.
		SprinklerHydraulicResponse first = postForResponse(handCalculatedRequest());
		SprinklerHydraulicResponse second = postForResponse(handCalculatedRequest());

		assertEquals(first.requiredPressureAtSupplyPsi(), second.requiredPressureAtSupplyPsi());
		assertEquals(first.totalDemandFlowGpm(), second.totalDemandFlowGpm());
	}

	private SprinklerHydraulicResponse postForResponse(SprinklerHydraulicRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), SprinklerHydraulicResponse.class);
	}

}
