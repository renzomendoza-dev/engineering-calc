package com.renzoproject.calc_api.mechanical.storage;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DomesticWaterStorageControllerTest {

	private static final String URL = "/api/mechanical/storage/domestic";
	private static final double DELTA = 1e-2;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void occupantLoad_twentyFourHourStorage_matchesExactRegressionValue() throws Exception {
		// 100 occupants * 150 LPCD (RESIDENTIAL_DWELLING, real lpcd-consumption.json) = 15000 L
		// daily demand; a 24-hour storage window with no safety margin reproduces it exactly.
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.OCCUPANT_LOAD, 100, "RESIDENTIAL_DWELLING", null, null, 24.0, 0.0);

		DomesticWaterStorageResponse response = postForResponse(request);

		assertEquals(15.0, response.requiredStorageVolumeM3(), DELTA);
	}

	@Test
	void fixtureUnit_flushTank_returns200() throws Exception {
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.FIXTURE_UNIT, null, null, 100.0, SystemTypeDto.FLUSH_TANK, 1.0, 0.0);

		DomesticWaterStorageResponse response = postForResponse(request);

		assertTrue(response.requiredStorageVolumeM3() > 0);
	}

	@Test
	void fixtureUnit_flushValve_returns200AndExceedsFlushTankVolume() throws Exception {
		// At WSFU=100, real wsfu-demand.json gives gpmFlushTanks=44, gpmFlushValves=68 -- the
		// flush-valve volume must come out higher for the same WSFU/duration.
		DomesticWaterStorageResponse flushTankResponse = postForResponse(new DomesticWaterStorageRequest(
				DemandBasisDto.FIXTURE_UNIT, null, null, 100.0, SystemTypeDto.FLUSH_TANK, 1.0, 0.0));
		DomesticWaterStorageResponse flushValveResponse = postForResponse(new DomesticWaterStorageRequest(
				DemandBasisDto.FIXTURE_UNIT, null, null, 100.0, SystemTypeDto.FLUSH_VALVE, 1.0, 0.0));

		assertTrue(flushValveResponse.requiredStorageVolumeM3() > flushTankResponse.requiredStorageVolumeM3());
	}

	@Test
	void occupantLoad_missingOccupantCount_returns400() throws Exception {
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.OCCUPANT_LOAD, null, "RESIDENTIAL_DWELLING", null, null, 24.0, 0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void occupantLoad_missingOccupancyType_returns400() throws Exception {
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.OCCUPANT_LOAD, 100, null, null, null, 24.0, 0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void fixtureUnit_missingTotalFixtureUnits_returns400() throws Exception {
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.FIXTURE_UNIT, null, null, null, SystemTypeDto.FLUSH_TANK, 1.0, 0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void fixtureUnit_missingSystemType_returns400() throws Exception {
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.FIXTURE_UNIT, null, null, 50.0, null, 1.0, 0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void occupantLoad_unknownOccupancyType_isCalcCoreValidation_returns400() throws Exception {
		// Structurally valid at the DTO boundary (non-blank string), but PerCapitaConsumptionResolver
		// doesn't recognize it -- must surface as 400 via CalculationException, not a Bean
		// Validation error.
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.OCCUPANT_LOAD, 100, "NOT_A_REAL_TYPE", null, null, 24.0, 0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void fixtureUnit_wsfuExceedsUpperBound_isCalcCoreValidation_returns400() throws Exception {
		DomesticWaterStorageRequest request = new DomesticWaterStorageRequest(
				DemandBasisDto.FIXTURE_UNIT, null, null, 10001.0, SystemTypeDto.FLUSH_TANK, 1.0, 0.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	private DomesticWaterStorageResponse postForResponse(DomesticWaterStorageRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DomesticWaterStorageResponse.class);
	}

}
