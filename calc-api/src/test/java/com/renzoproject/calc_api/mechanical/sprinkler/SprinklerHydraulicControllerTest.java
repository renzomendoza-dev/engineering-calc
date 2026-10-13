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
import static org.junit.jupiter.api.Assertions.assertNull;
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
		return new WaterSupplyDto(
				WaterSupplySourceDto.FLOW_TEST, new FlowTestSupplyDto(80.0, 60.0, 1000.0), null);
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
				List.of(new DeviceLossDto("Alarm valve", 5.0)),
				null, null);
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
				supply(), null, null, null);

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
				supply(), null, null, null);

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
				List.of(withFitting), supply(), null, null, null);

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
				List.of(withFitting), supply(), null, null, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("No published equivalent length")));
	}

	/** The hand-calculated network, with whatever supply (or none) the caller wants to try. */
	private static SprinklerHydraulicRequest requestWithSupply(WaterSupplyDto supply) {
		return new SprinklerHydraulicRequest(
				SprinklerSystemTypeDto.WET, SprinklerHazardDto.LIGHT, 0.10, 1500.0, 100.0, "RISER", 7.0,
				List.of(new RemoteAreaSprinklerDto("S1", 5.6, 130.0)),
				List.of(segment("S1", "B1", "1", 10.0, 0.0), segment("B1", "RISER", "2", 12.0, 10.0)),
				supply,
				List.of(new DeviceLossDto("Alarm valve", 5.0)),
				null, null);
	}

	@Test
	void gravityTankSupply_returns200_withTheHeadLessTheOutletRunsFriction() throws Exception {
		// 100 ft of water = 43.3 psi at no flow. At the 114.8162 gpm demand, 200 ft of 4" Sch 40
		// (ID 4.026 in) takes a little of it back; the demand side is unchanged from the flow-test
		// case, so only what the supply offers differs.
		SprinklerHydraulicResponse response = postForResponse(requestWithSupply(new WaterSupplyDto(
				WaterSupplySourceDto.GRAVITY_TANK,
				null,
				new GravityTankSupplyDto(100.0, List.of(
						new SupplyPipeRunDto("BI", "SCH40", "4", 120.0, 200.0, null))))));

		assertEquals(114.8162073, response.totalDemandFlowGpm(), 1e-6);
		assertEquals(17.110689, response.requiredPressureAtSupplyPsi(), 1e-6);
		assertTrue(
				response.availablePressureAtDemandPsi() < 43.3,
				"the outlet run should cost something: " + response.availablePressureAtDemandPsi());
		assertTrue(
				response.availablePressureAtDemandPsi() > 42.0,
				"200 ft of 4\" at 115 gpm shouldn't cost much: " + response.availablePressureAtDemandPsi());
		assertEquals(
				response.availablePressureAtDemandPsi() - response.requiredPressureAtSupplyPsi(),
				response.safetyMarginPsi(),
				1e-9);
		assertTrue(response.passes());
		assertTrue(response.supplyCurve().size() >= 5);
	}

	@Test
	void noWaterSupply_returns200_withTheDemandAndANullVerdict() throws Exception {
		SprinklerHydraulicResponse response = postForResponse(requestWithSupply(null));

		// Demand is identical to the flow-test case: it never depended on the supply.
		assertEquals(114.8162073, response.totalDemandFlowGpm(), 1e-6);
		assertEquals(17.110689, response.requiredPressureAtSupplyPsi(), 1e-6);
		assertEquals(2, response.segments().size());

		assertNull(response.availablePressureAtDemandPsi());
		assertNull(response.safetyMarginPsi());
		assertNull(response.safetyMarginPercent());
		assertNull(response.passes());
		assertTrue(response.supplyCurve().isEmpty());
		assertTrue(
				response.warnings().stream().anyMatch(w -> w.contains("No water supply given")),
				"the absent comparison should be stated: " + response.warnings());
	}

	@Test
	void supplyBlockNotMatchingItsSource_isBeanValidation_returns400() throws Exception {
		SprinklerHydraulicRequest request = requestWithSupply(new WaterSupplyDto(
				WaterSupplySourceDto.FLOW_TEST, null, new GravityTankSupplyDto(100.0, null)));

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				// Bean Validation names a class-level @AssertTrue after its getter, so the key is
				// the method's property name rather than the field's -- pinned here because the
				// frontend places inline errors by these keys.
				.andExpect(jsonPath("$.fieldErrors['waterSupply.sourceBlockPresent']")
						.value(org.hamcrest.Matchers.containsString("exactly the block matching")));
	}

	@Test
	void gravityTankAtOrBelowTheConnection_isBeanValidation_returns400() throws Exception {
		SprinklerHydraulicRequest request = requestWithSupply(new WaterSupplyDto(
				WaterSupplySourceDto.GRAVITY_TANK, null, new GravityTankSupplyDto(0.0, null)));

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors['waterSupply.gravityTank.waterLevelHeightFt']").exists());
	}

	@Test
	void omittedStartingPressure_isAccepted_andTheDesignFlowAndTheFloorGovern() throws Exception {
		// 0.10 gpm/ft2 over 130 ft2 = 13 gpm, which needs (13/5.6)^2 = 5.39 psi -- below the 7 psi
		// minimum operating pressure. With no starting pressure supplied, 7 psi must still govern, so
		// the sprinkler discharges 5.6 * sqrt(7) = 14.816 gpm, exactly as when 7.0 is passed.
		SprinklerHydraulicRequest withoutIt = new SprinklerHydraulicRequest(
				SprinklerSystemTypeDto.WET, SprinklerHazardDto.LIGHT, 0.10, 1500.0, 0.0, null, null,
				List.of(new RemoteAreaSprinklerDto("S1", 5.6, 130.0)),
				List.of(segment("S1", "RISER", "2", 10.0, 0.0)),
				supply(), null, null, null);
		SprinklerHydraulicRequest withSeven = new SprinklerHydraulicRequest(
				SprinklerSystemTypeDto.WET, SprinklerHazardDto.LIGHT, 0.10, 1500.0, 0.0, null, 7.0,
				List.of(new RemoteAreaSprinklerDto("S1", 5.6, 130.0)),
				List.of(segment("S1", "RISER", "2", 10.0, 0.0)),
				supply(), null, null, null);

		SprinklerHydraulicResponse omitted = postForResponse(withoutIt);

		assertEquals(14.8162073, omitted.segments().get(0).flowGpm(), 1e-6);
		assertEquals(
				postForResponse(withSeven).requiredPressureAtSupplyPsi(),
				omitted.requiredPressureAtSupplyPsi(),
				0.0);
	}

	/** The Everstrong workbook's Production Office network: BL-1 listed, BL-2 and BL-3 declared identical. */
	private static SprinklerHydraulicRequest workbookRequest(List<IdenticalBranchDto> copies) {
		return new SprinklerHydraulicRequest(
				SprinklerSystemTypeDto.WET, SprinklerHazardDto.ORDINARY_GROUP_1, 0.15, 1500.0, 200.0, "BOR", 7.0,
				List.of(
						new RemoteAreaSprinklerDto("BL1-S1", 5.6, 130.0),
						new RemoteAreaSprinklerDto("BL1-S2", 5.6, 130.0),
						new RemoteAreaSprinklerDto("BL1-S3", 5.6, 130.0),
						new RemoteAreaSprinklerDto("BL1-S4", 5.6, 130.0)),
				List.of(
						withFittingsFt("BL1-S1", "BL1-S2", "1", 10.0, 2.0, 0.0),
						withFittingsFt("BL1-S2", "BL1-S3", "1", 8.0, 5.0, 0.0),
						withFittingsFt("BL1-S3", "BL1-S4", "1-1/4", 10.0, 2.0, 0.0),
						withFittingsFt("BL1-S4", "A", "1-1/4", 10.0, 2.0, 0.0),
						withFittingsFt("A", "B", "3", 10.0, 8.0, 0.0),
						withFittingsFt("B", "C", "3", 10.0, 8.0, 0.0),
						withFittingsFt("C", "TOR", "3", 100.0, 8.0, 0.0),
						// The workbook totals "lgth 246 + ftg 13 + elev 20"; pipe length is the centerline,
						// vertical run included, so 246 + 20 goes in as 266.
						withFittingsFt("TOR", "R2", "3", 266.0, 13.0, 20.0),
						withFittingsFt("R2", "BOR", "4", 50.0, 31.0, 0.0)),
				new WaterSupplyDto(WaterSupplySourceDto.FLOW_TEST, new FlowTestSupplyDto(120.0, 100.0, 1000.0), null),
				null,
				copies, null);
	}

	private static SprinklerPipeSegmentDto withFittingsFt(
			String from, String to, String size, double lengthFt, double fittingsFt, double elevationFt) {
		return new SprinklerPipeSegmentDto(from, to, "BI", "SCH40", size, 120.0, lengthFt, elevationFt,
				List.of(new SegmentFittingDto(null, 1, fittingsFt)));
	}

	@Test
	void identicalBranches_matchTheWorkbook_andComeBackAsOneBranchLevelLineEach() throws Exception {
		// BL-2 and BL-3 are never listed. Workbook: 92.416 gpm @ 30.499 psi, 93.568 gpm @ 31.264 psi,
		// 278.08 gpm @ 41.07 psi at the top of the riser, 77.01 psi at the bottom, 478.08 gpm with the
		// hose. The workbook rounds pipe bores to two decimals, so these agree to about a tenth of a
		// percent rather than to the digit.
		SprinklerHydraulicResponse response = postForResponse(workbookRequest(List.of(
				new IdenticalBranchDto("B", "BL1-S4"), new IdenticalBranchDto("C", "BL1-S4"))));

		assertEquals(2, response.identicalBranches().size());
		IdenticalBranchRowDto bl2 = response.identicalBranches().get(0);
		assertEquals("B", bl2.joinNodeId());
		assertEquals("BL1-S4", bl2.referenceNodeId());
		assertEquals("A", bl2.referenceJunctionNodeId());
		assertEquals(4, bl2.sprinklerCount());
		assertEquals(92.416, bl2.flowGpm(), 92.416 * 0.003);
		assertEquals(30.499, bl2.junctionPressurePsi(), 30.499 * 0.003);
		assertEquals(
				bl2.referenceFlowGpm() * Math.sqrt(bl2.junctionPressurePsi() / bl2.referencePressurePsi()),
				bl2.flowGpm(),
				1e-9);
		assertEquals(93.568, response.identicalBranches().get(1).flowGpm(), 93.568 * 0.003);

		assertEquals(478.080, response.totalDemandFlowGpm(), 478.080 * 0.003);
		assertEquals(77.011, response.requiredPressureAtSupplyPsi(), 77.011 * 0.003);
		// Only BL-1's sprinkler pipes, the cross mains and the riser have rows.
		assertEquals(9, response.segments().size());
	}

	@Test
	void identicalBranch_pointingAtAnUnknownJunction_isCalcCoreValidation_returns400() throws Exception {
		SprinklerHydraulicRequest request = workbookRequest(List.of(new IdenticalBranchDto("NOWHERE", "BL1-S4")));

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("NOWHERE")));
	}

	@Test
	void identicalBranch_withABlankNode_isBeanValidation_returns400() throws Exception {
		SprinklerHydraulicRequest request = workbookRequest(List.of(new IdenticalBranchDto("B", " ")));

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors['identicalBranches[0].referenceNodeId']").exists());
	}

	@Test
	void withoutIdenticalBranches_theResponseCarriesAnEmptyList() throws Exception {
		SprinklerHydraulicResponse response = postForResponse(handCalculatedRequest());

		assertTrue(response.identicalBranches().isEmpty());
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

	@Test
	void response_listsEachSprinklersOwnFlowAndPressure() throws Exception {
		SprinklerHydraulicResponse response = postForResponse(handCalculatedRequest());

		assertEquals(1, response.sprinklers().size());
		SprinklerFlowRowDto sprinkler = response.sprinklers().get(0);
		assertEquals("S1", sprinkler.nodeId());
		assertEquals(5.6, sprinkler.kFactor(), 1e-12);
		assertEquals(14.8162073, sprinkler.flowGpm(), 1e-6);
		assertEquals(7.0, sprinkler.pressurePsi(), 1e-9);
		// density 0.10 gpm/ft2 over its 130 ft2
		assertEquals(13.0, sprinkler.minimumFlowGpm(), 1e-9);
	}
}
