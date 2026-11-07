package com.renzoproject.calc_api.mechanical.standpipe;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StandpipeHydraulicControllerTest {

	private static final String URL = "/api/mechanical/standpipe/hydraulics";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static StandpipeSegmentDto hoseStation() {
		return new StandpipeSegmentDto("A", "HOSE_PIPING", 100.0, "ONE_AND_HALF_INCH", "BI", "SCH40", "1-1/2", 120.0, 30.0, 0.0, null);
	}

	private static StandpipeSegmentDto riser() {
		return new StandpipeSegmentDto("Riser", "STANDPIPE", null, null, "BI", "SCH40", "3", 120.0, 400.0, 5.0,
				List.of(new StandpipeFittingDto("ELBOW_90_STANDARD", 2, null), new StandpipeFittingDto("GATE_VALVE", 1, null),
						new StandpipeFittingDto("SWING_CHECK_VALVE", 1, null)));
	}

	private static StandpipeHydraulicRequest carmona() {
		return new StandpipeHydraulicRequest("CLASS_II", "NONE", null, null, 0, false, false, 0, false, false, null, null, 60.0, 50.0, List.of(hoseStation(), riser()));
	}

	@Test
	void carmonaHoseStation_matchesTheWorkbook() throws Exception {
		StandpipeHydraulicResponse response = postForResponse(carmona());

		assertEquals(82.77, response.pressureAtSupplyPsi(), 0.3);
		assertEquals(150.0, response.flowAtSupplyGpm(), 1e-9);
		assertEquals(9_000.0, response.waterSupplyVolumeGal(), 1e-6);
		assertEquals(100.0, response.requiredFlowGpm(), 1e-9);
		assertEquals(2, response.segments().size());
		assertEquals("ONE_AND_HALF_INCH", response.segments().get(0).hoseConnectionSize());
		assertEquals(31.0, response.segments().get(1).fittingsLengthFt(), 1e-9);
		assertTrue(response.checks().stream().anyMatch(c -> c.clause().equals("10.2.6.1") && c.status().equals("PASS")));
	}

	@Test
	void flowAddedAtSupply_isWorkedOutForClassIWhenLeftBlank() throws Exception {
		var connection = new StandpipeSegmentDto("Top", "HOSE_PIPING", 250.0, "TWO_AND_HALF_INCH", "BI", "SCH40", "2-1/2", 120.0, 5.0, 0.0, null);
		var second = new StandpipeSegmentDto("Second", "HOSE_PIPING", 250.0, "TWO_AND_HALF_INCH", "BI", "SCH40", "2-1/2", 120.0, 5.0, 0.0, null);
		var standpipe = new StandpipeSegmentDto("Standpipe", "STANDPIPE", null, null, "BI", "SCH40", "4", 120.0, 100.0, 40.0, null);
		var request = new StandpipeHydraulicRequest("CLASS_I", "NONE", null, null, 1, false, false, 0, false, false, null, null, null, null, List.of(connection, second, standpipe));

		StandpipeHydraulicResponse response = postForResponse(request);

		assertTrue(response.flowAddedAtSupplyAutomatic());
		assertEquals(250.0, response.flowAddedAtSupplyGpm(), 1e-9);
		assertEquals(750.0, response.flowAtSupplyGpm(), 1e-9);
	}

	@Test
	void manualSystemLateralsAndInletSize_areCarriedThrough() throws Exception {
		var connection = new StandpipeSegmentDto("Top", "HOSE_PIPING", 250.0, "TWO_AND_HALF_INCH", "BI", "SCH40", "2-1/2", 120.0, 5.0, 0.0, null);
		var second = new StandpipeSegmentDto("Second", "HOSE_PIPING", 250.0, "TWO_AND_HALF_INCH", "BI", "SCH40", "2-1/2", 120.0, 5.0, 0.0, null);
		var standpipe = new StandpipeSegmentDto("Standpipe", "STANDPIPE", null, null, "BI", "SCH40", "4", 120.0, 100.0, 40.0, null);
		var request = new StandpipeHydraulicRequest("CLASS_I", "NONE", null, null, 0, false, false, 1, true, true, "FOUR_INCH", null, null, null,
				List.of(connection, second, standpipe));

		StandpipeHydraulicResponse response = postForResponse(request);

		assertEquals(750.0, response.requiredFlowGpm(), 1e-9);
		assertTrue(response.manualSystem());
		assertEquals("FOUR_INCH", response.fdcInletSize());
		assertEquals(2, response.requiredFdcInlets());
		assertTrue(response.checks().stream().anyMatch(c -> c.clause().equals("10.2.6.1.2")));
		assertTrue(response.checks().stream().anyMatch(c -> c.clause().equals("10.6.1.2.1.1")));
	}

	@Test
	void manualClassII_andUnknownInletSize_return400() throws Exception {
		var manualClassII = new StandpipeHydraulicRequest("CLASS_II", null, null, null, 0, false, false, 0, false, true, null, null, null, null,
				List.of(hoseStation(), riser()));
		var badSize = new StandpipeHydraulicRequest("CLASS_II", null, null, null, 0, false, false, 0, false, false, "SIX_INCH", null, null, null,
				List.of(hoseStation(), riser()));

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(manualClassII)))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(badSize)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void optionalFieldsMayBeLeftOut() throws Exception {
		var request = new StandpipeHydraulicRequest("CLASS_II", null, null, null, 0, false, false, 0, false, false, null, null, null, null, List.of(hoseStation(), riser()));

		StandpipeHydraulicResponse response = postForResponse(request);

		assertEquals(30.0, response.supplyDurationMinutes(), 1e-9);
		assertEquals(100.0, response.flowAtSupplyGpm(), 1e-9);
	}

	@Test
	void referenceTables_areServedWithTheirEdition() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/mechanical/standpipe/reference")).andExpect(status().isOk()).andReturn();
		StandpipeReferenceResponse reference = objectMapper.readValue(result.getResponse().getContentAsString(), StandpipeReferenceResponse.class);

		assertTrue(reference.edition().contains("2024"));
		assertEquals(9, reference.fittings().size());
		assertEquals(7.0, reference.fittings().stream().filter(f -> f.fittingType().equals("ELBOW_90_STANDARD")).findFirst().orElseThrow()
				.equivalentLengthFtByNominalSize().get("3"), 1e-9);
		assertEquals(8, reference.cFactors().size());
	}

	@Test
	void unknownClass_returns400() throws Exception {
		var request = new StandpipeHydraulicRequest("CLASS_IV", null, null, null, 0, false, false, 0, false, false, null, null, null, null, List.of(hoseStation()));

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void noSegments_returns400() throws Exception {
		var request = new StandpipeHydraulicRequest("CLASS_II", null, null, null, 0, false, false, 0, false, false, null, null, null, null, List.of());

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknownFitting_returns400() throws Exception {
		var segment = new StandpipeSegmentDto("A", "HOSE_PIPING", 100.0, "ONE_AND_HALF_INCH", "BI", "SCH40", "1-1/2", 120.0, 30.0, 0.0,
				List.of(new StandpipeFittingDto("NOPE", 1, null)));
		var request = new StandpipeHydraulicRequest("CLASS_II", null, null, null, 0, false, false, 0, false, false, null, null, null, null, List.of(segment));

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	private StandpipeHydraulicResponse postForResponse(StandpipeHydraulicRequest request) throws Exception {
		MvcResult result = mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readValue(result.getResponse().getContentAsString(), StandpipeHydraulicResponse.class);
	}

}
