package com.renzoproject.calc_api.electrical.generatorset;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GeneratorSetSizingControllerTest {

	private static final String URL = "/api/electrical/generator-set-sizing";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static GeneratorSetSizingRequest request(String standbyClass, String optionalTransfer, String occupancy) {
		return new GeneratorSetSizingRequest(
				standbyClass, optionalTransfer, "THREE_PHASE", 230,
				List.of(new LightingAreaDto(null, occupancy, null, 1000, null, false, true)),
				List.of(new ReceptacleGroupDto(null, 100, 1000.0, false)),
				true,
				List.of(),
				List.of(new MotorLoadDto(null, "10", 230, 1, null, null)),
				0.8, 0, null, false,
				new OutputConductorDto(30.0, 3, 1, "THHN", "COPPER", 75),
				null, null, null, null);
	}

	@Test
	void officeEmergencyLoad_isSizedEndToEnd() throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request("EMERGENCY", null, "OFFICE_BUILDINGS"))))
				.andExpect(status().isOk())
				.andReturn();

		GeneratorSetSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), GeneratorSetSizingResponse.class);

		double motorVa = Math.sqrt(3.0) * 230 * 28;
		assertEquals((28_000 + 14_000 + motorVa) / 1000, response.demandKva(), DELTA);
		assertEquals(60.0, response.recommendedStandardKva(), DELTA);
		assertEquals(60_000 / (Math.sqrt(3.0) * 230) * 1.15, response.minimumOutputConductorAmpacityAmps(), DELTA);
		assertNotNull(response.outputConductor());
		assertEquals("7.0.1.4(A)", response.capacityCitation());
		assertTrue(response.loadRows().stream().anyMatch(row -> row.kind().equals("MOTOR")));
		assertTrue(response.requirements().size() > 3);
	}

	@Test
	void unknownStandbyClass_isABadRequest() throws Exception {
		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request("BACKUP", null, "OFFICE_BUILDINGS"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Unknown standby class: BACKUP"));
	}

	@Test
	void unknownOccupancy_isABadRequest() throws Exception {
		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request("EMERGENCY", null, "MOON_BASE"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Unknown lighting occupancy: MOON_BASE"));
	}

	@Test
	void optionalStandbyWithoutATransferMode_isABadRequest() throws Exception {
		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request("OPTIONAL", null, "OFFICE_BUILDINGS"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void invalidFloorArea_isReportedAgainstTheListItem() throws Exception {
		GeneratorSetSizingRequest bad = new GeneratorSetSizingRequest(
				"EMERGENCY", null, "THREE_PHASE", 230,
				List.of(new LightingAreaDto(null, "OFFICE_BUILDINGS", null, 0, null, false, true)),
				List.of(), true, List.of(), List.of(), 0.8, 0, null, false, null, null, null, null, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(bad)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors['lightingAreas[0].floorAreaM2']").exists());
	}

	@Test
	void motorStartingCheck_runsWhenReactanceAndDipAreGiven() throws Exception {
		GeneratorSetSizingRequest base = request("EMERGENCY", null, "OFFICE_BUILDINGS");
		GeneratorSetSizingRequest withStarting = new GeneratorSetSizingRequest(
				base.standbyClass(), null, base.phaseType(), base.voltage(), List.of(), List.of(), true, List.of(),
				List.of(new MotorLoadDto("Pump", "10", 230, 1, 1, 100.0)),
				0.8, 0, null, false, null, 20.0, 15.0, null, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(withStarting)))
				.andExpect(status().isOk())
				.andReturn();
		GeneratorSetSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), GeneratorSetSizingResponse.class);

		double startingKva = Math.sqrt(3.0) * 230 * 162 / 1000;
		assertEquals(1, response.startingSteps().size());
		assertEquals(startingKva, response.startingSteps().get(0).startingKva(), DELTA);
		assertEquals(0.20 * startingKva * (1 / 0.15 - 1), response.requiredKva(), DELTA);
		assertEquals("MOTOR_STARTING", response.governingCase());
		assertNotNull(response.startingBasis());
	}

	@Test
	void startStepAndStartingCurrentCanBeLeftBlank() throws Exception {
		// The first slice's request shape: no step, no percentage, no reactance, no dip.
		String body = "{\"standbyClass\":\"EMERGENCY\",\"phaseType\":\"THREE_PHASE\",\"voltage\":230,"
				+ "\"applyReceptacleDemandFactor\":true,\"designPreventsOverload\":false,"
				+ "\"motors\":[{\"horsepowerLabel\":\"10\",\"voltage\":230,\"quantity\":1}],"
				+ "\"generatorPowerFactor\":0.8,\"spareCapacityPercent\":0}";

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.governingCase").value("RUNNING_LOAD"))
				.andExpect(jsonPath("$.startingSteps.length()").value(0));
	}

	@Test
	void reactanceWithoutADipLimit_isABadRequest() throws Exception {
		String body = "{\"standbyClass\":\"EMERGENCY\",\"phaseType\":\"THREE_PHASE\",\"voltage\":230,"
				+ "\"applyReceptacleDemandFactor\":true,\"designPreventsOverload\":false,"
				+ "\"motors\":[{\"horsepowerLabel\":\"10\",\"voltage\":230,\"quantity\":1}],"
				+ "\"generatorPowerFactor\":0.8,\"spareCapacityPercent\":0,\"subtransientReactancePercent\":20}";

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void totalBuildingLoadAndFirePump_canBeEnteredDirectly() throws Exception {
		DirectLoadDto tower = new DirectLoadDto("Tower", 250, "KW", 0.85, true);
		GeneratorSetSizingRequest direct = new GeneratorSetSizingRequest(
				"EMERGENCY", null, "THREE_PHASE", 230, List.of(), List.of(), true, List.of(), List.of(),
				0.8, 0, null, false, null, null, null, List.of(tower), null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(direct)))
				.andExpect(status().isOk())
				.andReturn();
		GeneratorSetSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), GeneratorSetSizingResponse.class);

		assertEquals(250 / 0.85, response.demandKva(), DELTA);
		assertEquals("BUILDING", response.loadRows().get(0).kind());
		assertEquals(300.0, response.recommendedStandardKva(), DELTA);
	}

	@Test
	void firePump_isSizedWithItsArticle6_95Checks() throws Exception {
		DirectLoadDto tower = new DirectLoadDto("Tower", 20, "KVA", null, true);
		FirePumpDto pump = new FirePumpDto(
				new PumpMotorDto(null, null, 40.0, 240.0),
				new PumpMotorDto("3", 400, null, null),
				2.0, 1, null,
				new SupplyRunDto(30.0, 3, 1, "THHN", "COPPER", 75, 300.0, "PVC", 0.3, 0.85, 5.0));
		GeneratorSetSizingRequest request = new GeneratorSetSizingRequest(
				"EMERGENCY", null, "THREE_PHASE", 400, List.of(), List.of(), true, List.of(), List.of(),
				0.8, 0, null, false, null, 20.0, 15.0, List.of(tower), pump);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();
		GeneratorSetSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), GeneratorSetSizingResponse.class);

		FirePumpResultDto firePump = response.firePump();
		assertNotNull(firePump);
		assertEquals(40.0, firePump.pumpFullLoadAmps(), DELTA);
		// The 3 hp jockey at 400 V draws 6.1 A (Table 4.30.14.4).
		assertEquals(6.1, firePump.jockeyFullLoadAmps(), DELTA);
		assertEquals(1.25 * (40.0 + 6.1) + 2.0, firePump.minimumConductorAmpacityAmps(), DELTA);
		assertNotNull(firePump.conductor());
		assertTrue(firePump.conductor().upsizedForVoltageDrop());
		assertTrue(firePump.runningDropWithinLimit());
		assertEquals(15.0 - firePump.cableStartingDropPercent(), response.startingSteps().get(0).dipLimitPercent(), DELTA);
		assertTrue(firePump.requirements().stream().anyMatch(item -> item.citation().equals("6.95.1.4(B)(2)(b)")));
		assertTrue(response.loadRows().stream().anyMatch(row -> row.kind().equals("FIRE_PUMP")));
	}

	@Test
	void aPumpEnteredTwoWays_isABadRequest() throws Exception {
		FirePumpDto pump = new FirePumpDto(new PumpMotorDto("10", 230, 28.0, null), null, null, null, null, null);
		GeneratorSetSizingRequest request = new GeneratorSetSizingRequest(
				"EMERGENCY", null, "THREE_PHASE", 230, List.of(), List.of(), true, List.of(), List.of(),
				0.8, 0, null, false, null, null, null, null, pump);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Enter a pump either by horsepower and voltage or by its full-load current"));
	}

	@Test
	void aTotalLoadInKwWithoutAPowerFactor_isABadRequest() throws Exception {
		DirectLoadDto bad = new DirectLoadDto("Tower", 250, "KW", null, true);
		GeneratorSetSizingRequest request = new GeneratorSetSizingRequest(
				"EMERGENCY", null, "THREE_PHASE", 230, List.of(), List.of(), true, List.of(), List.of(),
				0.8, 0, null, false, null, null, null, List.of(bad), null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("a load in kW needs a power factor greater than 0 and at most 1"));
	}

	@Test
	void unknownTotalLoadUnit_isABadRequest() throws Exception {
		DirectLoadDto bad = new DirectLoadDto("x", 10, "HP", null, true);
		GeneratorSetSizingRequest request = new GeneratorSetSizingRequest(
				"EMERGENCY", null, "THREE_PHASE", 230, List.of(), List.of(), true, List.of(), List.of(),
				0.8, 0, null, false, null, null, null, List.of(bad), null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Unknown total load unit: HP"));
	}

	@Test
	void reference_listsTheChoicesTheFormOffers() throws Exception {
		mockMvc.perform(get(URL + "/reference"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lightingOccupancies.length()").value(17))
				.andExpect(jsonPath("$.standbyClasses.length()").value(3))
				.andExpect(jsonPath("$.optionalTransfers.length()").value(3))
				.andExpect(jsonPath("$.standardSizesKva[0]").value(10.0))
				.andExpect(jsonPath("$.defaultGeneratorPowerFactor").value(0.8))
				.andExpect(jsonPath("$.defaultSubtransientReactancePercent").value(20.0))
				.andExpect(jsonPath("$.defaultMaximumVoltageDipPercent").value(15.0))
				.andExpect(jsonPath("$.defaultFirePumpStartingPowerFactor").value(0.3))
				.andExpect(jsonPath("$.defaultFirePumpRunningPowerFactor").value(0.85))
				.andExpect(jsonPath("$.defaultCableStartingDropAllowancePercent").value(5.0));
	}

}
