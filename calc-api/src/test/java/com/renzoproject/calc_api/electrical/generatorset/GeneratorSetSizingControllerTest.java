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
				List.of(new MotorLoadDto(null, "10", 230, 1)),
				0.8, 0, null, false,
				new OutputConductorDto(30.0, 3, 1, "THHN", "COPPER", 75));
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
				List.of(), true, List.of(), List.of(), 0.8, 0, null, false, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(bad)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors['lightingAreas[0].floorAreaM2']").exists());
	}

	@Test
	void reference_listsTheChoicesTheFormOffers() throws Exception {
		mockMvc.perform(get(URL + "/reference"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lightingOccupancies.length()").value(17))
				.andExpect(jsonPath("$.standbyClasses.length()").value(3))
				.andExpect(jsonPath("$.optionalTransfers.length()").value(3))
				.andExpect(jsonPath("$.standardSizesKva[0]").value(10.0))
				.andExpect(jsonPath("$.defaultGeneratorPowerFactor").value(0.8));
	}

}
