package com.renzoproject.calc_api.mechanical.reference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StorageReferenceControllerTest {

	private static final String BASE_URL = "/api/mechanical/reference";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void lpcdConsumptionTable_returns200WithResidentialDwellingAt150() throws Exception {
		mockMvc.perform(get(BASE_URL + "/lpcd-consumption-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[?(@.type == 'RESIDENTIAL_DWELLING')].lpcd").value(contains(150.0)));
	}

	@Test
	void wsfuDemandTable_returns200WithKnownWorkedExampleRow() throws Exception {
		// Same WSFU=100 row the DomesticWaterStorageControllerTest worked examples key off of.
		mockMvc.perform(get(BASE_URL + "/wsfu-demand-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[?(@.wsfu == 100.0)].gpmFlushTanks").value(contains(44.0)))
				.andExpect(jsonPath("$[?(@.wsfu == 100.0)].gpmFlushValves").value(contains(68.0)));
	}

	@Test
	void fireWaterDurationTable_returns200WithOrdinaryHazardSixtyToNinety() throws Exception {
		mockMvc.perform(get(BASE_URL + "/fire-water-duration-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[?(@.hazardClassification == 'ORDINARY_HAZARD')].minMinutes").value(contains(60.0)))
				.andExpect(jsonPath("$[?(@.hazardClassification == 'ORDINARY_HAZARD')].maxMinutes").value(contains(90.0)));
	}

}
