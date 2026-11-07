package com.renzoproject.calc_api.smokecontrol.naturalventing;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NaturalVentingControllerTest {

	private static final String URL = "/api/smoke-control/natural-venting";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static VentZoneDto annexDZone() {
		return new VentZoneDto("Retail storage", 9.1, null, null, 73.0, 73.0, 0.61, 16, null, null, null, 18.3, 9.05, null, 1.5, null,
				null, null, null, null);
	}

	private static FireCaseDto annexDFire(String name, double q, double layerHeight) {
		return new FireCaseDto(name, "Retail storage", "STEADY", q, null, null, null, null, null, null, null, null, 2136.0, null, 0.5, layerHeight);
	}

	@Test
	void annexDSampleProblem_cases1And8_matchTableD8() throws Exception {
		NaturalVentingRequest request = new NaturalVentingRequest(294.0 - 273.15, 1.2, 1.0, null, null, List.of(annexDZone()),
				List.of(annexDFire("Case 1", 100_000, 3.0), annexDFire("Case 8", 34_400, 6.0)));

		NaturalVentingResponse response = postForResponse(request);

		assertEquals(2, response.fireCases().size());
		var case1 = response.fireCases().get(0);
		assertEquals("REACHES_LAYER", case1.regime());
		assertEquals(62.8, case1.plumeMassFlow(), 0.5);
		assertEquals(557.0, case1.layerTemperatureRise(), 2.0);
		assertEquals(17.6, case1.requiredVentArea(), 0.3);
		assertEquals(28.3, response.fireCases().get(1).requiredVentArea(), 0.3);
		assertEquals(1, response.zones().size());
		assertTrue(response.zones().get(0).checks().stream().anyMatch(c -> c.clause().equals("5.4.2") && c.status().equals("PASS")));
	}

	@Test
	void growingFire_withDefaults_usesTheGrowthConstant1055() throws Exception {
		FireCaseDto growing = new FireCaseDto("Growing", "Retail storage", "GROWING", null, 150.0, 90.0, null, null, 360.0, null, 3.0, null, null, null,
				null, 6.0);
		NaturalVentingRequest request = new NaturalVentingRequest(20.0, null, null, null, null, List.of(annexDZone()), List.of(growing));

		NaturalVentingResponse response = postForResponse(request);

		assertEquals(9_495.0, response.fireCases().get(0).designHeatReleaseRate(), 1e-6);
	}

	@Test
	void growingFire_withASmokeDetector_hasItsDetectionTimeWorkedOut() throws Exception {
		FireCaseDto byDetector = new FireCaseDto("Detector", "Retail storage", "GROWING", null, 150.0, null, 6.5, null, 300.0, null, 3.0, null, null, null,
				null, 6.0);
		NaturalVentingRequest request = new NaturalVentingRequest(20.0, null, null, null, null, List.of(annexDZone()), List.of(byDetector));

		FireCaseResultDto fire = postForResponse(request).fireCases().get(0);

		// H = 9.1 m (the fire is at floor level), r = 6.5 m, tg = 150 s, the 20 C minimum rise: 138.4 s.
		assertEquals(138.4, fire.detectionTime(), 0.2);
		assertTrue(fire.detectionTimeFromDetector());
		assertEquals(1055 * Math.pow((300 + fire.detectionTime()) / 150, 2), fire.designHeatReleaseRate(), 1e-6);
		assertTrue(fire.checks().stream().anyMatch(c -> c.clause().equals("9.2.5.4.3") && c.status().equals("INFO")));
	}

	@Test
	void theImageFireScreening_isReturnedForEveryFireCase() throws Exception {
		NaturalVentingRequest request = new NaturalVentingRequest(20.0, null, null, null, null, List.of(annexDZone()),
				List.of(annexDFire("Case 8", 34_400, 6.0)));

		FireCaseResultDto fire = postForResponse(request).fireCases().get(0);

		assertEquals(2.0, fire.imageFire().wall().multiplier(), 1e-9);
		assertEquals(4.0, fire.imageFire().corner().multiplier(), 1e-9);
		assertEquals(4 * 34_400.0, fire.imageFire().corner().equivalentHeatReleaseRate(), 1e-6);
		assertTrue(fire.imageFire().corner().layerTemperatureRise() > fire.imageFire().wall().layerTemperatureRise());
		assertTrue(fire.imageFire().wall().layerTemperatureRise() > fire.layerTemperatureRise());
		assertTrue(fire.checks().stream().anyMatch(c -> c.clause().equals("Image fire (wall)") && c.basis().equals("SUPPLEMENTARY")));
	}

	@Test
	void theTimeSteppingCheck_isCarriedThroughForAGrowingFireInAGableZoneWithARaisedMonitor() throws Exception {
		// The IN1GO warehouse 1: ridge 12 m, eave 8 m, a raised monitor with its vent centre at 12.75 m, K = 1.0.
		VentZoneDto warehouse = new VentZoneDto("W1", 12.0, 12.75, null, 60.0, 20.0, 0.25, null, 144.0, null, null, null, null, 0.25, null, 128.0,
				null, null, null, 8.0);
		FireCaseDto ultrafast = new FireCaseDto("Ultrafast", "W1", "GROWING", null, 75.0, 90.0, null, null, 360.0, null, null, null, 1133.33, null, 0.0, 7.0);
		NaturalVentingRequest request = new NaturalVentingRequest(40.0, 1.127, 1.0, 1.0, null, List.of(warehouse), List.of(ultrafast));

		FireCaseResultDto fire = postForResponse(request).fireCases().get(0);

		assertEquals(450.0, fire.transientInterface().endTime(), 1e-9);
		assertEquals(10.2, fire.transientInterface().interfaceHeightAtEnd(), 0.05);
		assertEquals(646.0, fire.transientInterface().timeToDesignHeight(), 1.5);
		assertEquals(174.0, fire.transientInterface().timeToDesignHeightWithoutVents(), 1.5);
		assertTrue(fire.checks().stream().anyMatch(c -> c.clause().equals("Time-stepping") && c.status().equals("PASS")));
	}

	@Test
	void theTimeSteppingCheck_isLeftOutForASteadyFire() throws Exception {
		NaturalVentingRequest request = new NaturalVentingRequest(20.0, null, null, null, null, List.of(annexDZone()),
				List.of(annexDFire("Case 8", 34_400, 6.0)));

		assertEquals(null, postForResponse(request).fireCases().get(0).transientInterface());
	}

	@Test
	void anEaveAboveTheRidge_returns400() throws Exception {
		VentZoneDto bad = new VentZoneDto("W1", 12.0, 12.75, null, 60.0, 20.0, 0.25, null, 144.0, null, null, null, null, 0.25, null, 128.0,
				null, null, null, 13.0);
		FireCaseDto fire = new FireCaseDto("Ultrafast", "W1", "GROWING", null, 75.0, 90.0, null, null, 360.0, null, null, null, 1133.33, null, 0.0, 7.0);
		NaturalVentingRequest request = new NaturalVentingRequest(40.0, null, null, null, null, List.of(bad), List.of(fire));

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void growingFire_withAGivenDetectionTime_reportsItAsGiven() throws Exception {
		FireCaseDto growing = new FireCaseDto("Growing", "Retail storage", "GROWING", null, 150.0, 90.0, null, null, 360.0, null, 3.0, null, null, null,
				null, 6.0);
		NaturalVentingRequest request = new NaturalVentingRequest(20.0, null, null, null, null, List.of(annexDZone()), List.of(growing));

		FireCaseResultDto fire = postForResponse(request).fireCases().get(0);

		assertEquals(90.0, fire.detectionTime(), 1e-9);
		assertFalse(fire.detectionTimeFromDetector());
	}

	@Test
	void detectionTimeAndDetectorDistanceTogether_return400() throws Exception {
		FireCaseDto both = new FireCaseDto("Both", "Retail storage", "GROWING", null, 150.0, 90.0, 6.5, null, 360.0, null, 3.0, null, null, null, null, 6.0);

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new NaturalVentingRequest(20.0, null, null, null, null, List.of(annexDZone()), List.of(both)))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknownFireKind_returns400() throws Exception {
		FireCaseDto bad = new FireCaseDto("Bad", "Retail storage", "FAST", 1000.0, null, null, null, null, null, null, 1.0, null, null, null, null, 3.0);

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new NaturalVentingRequest(20.0, null, null, null, null, List.of(annexDZone()), List.of(bad)))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void fireCaseInUnknownZone_returns400() throws Exception {
		FireCaseDto lost = new FireCaseDto("Lost", "Nowhere", "STEADY", 1000.0, null, null, null, null, null, null, 1.0, null, null, null, null, 3.0);

		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new NaturalVentingRequest(20.0, null, null, null, null, List.of(annexDZone()), List.of(lost)))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void noZones_returns400() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new NaturalVentingRequest(20.0, null, null, null, null, List.of(), List.of(annexDFire("A", 1000.0, 3.0))))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingAmbientTemperature_returns400() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new NaturalVentingRequest(null, null, null, null, null, List.of(annexDZone()), List.of(annexDFire("A", 1000.0, 3.0))))))
				.andExpect(status().isBadRequest());
	}

	private NaturalVentingResponse postForResponse(NaturalVentingRequest request) throws Exception {
		MvcResult result = mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readValue(result.getResponse().getContentAsString(), NaturalVentingResponse.class);
	}

}
