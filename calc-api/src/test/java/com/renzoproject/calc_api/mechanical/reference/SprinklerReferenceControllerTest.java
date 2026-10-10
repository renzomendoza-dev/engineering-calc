package com.renzoproject.calc_api.mechanical.reference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms the four sprinkler reference tables return the full contents of their
 * reference/sprinkler/*.json files, including the confidence tags a form needs in order to badge
 * unverified values.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SprinklerReferenceControllerTest {

	private static final String BASE_URL = "/api/mechanical/reference";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void hazardPresetsTable_returnsOneRowPerHazardWithConfidenceTags() throws Exception {
		mockMvc.perform(get(BASE_URL + "/sprinkler-hazard-presets-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$", hasSize(5)))
				.andExpect(jsonPath("$[?(@.hazard == 'LIGHT')].densityGpmPerSqFt").value(contains(0.10)))
				.andExpect(jsonPath("$[?(@.hazard == 'LIGHT')].designAreaSqFt").value(contains(1500.0)))
				.andExpect(jsonPath("$[?(@.hazard == 'EXTRA_GROUP_2')].densityGpmPerSqFt").value(contains(0.40)))
				.andExpect(jsonPath("$[?(@.confidence == 'verify')]", hasSize(5)));
	}

	@Test
	void cFactorsTable_returnsThePublishedSet() throws Exception {
		mockMvc.perform(get(BASE_URL + "/sprinkler-c-factors-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(6)))
				.andExpect(jsonPath("$[?(@.pipeType == 'STEEL' && @.systemType == 'WET')].cFactor")
						.value(contains(120.0)))
				.andExpect(jsonPath("$[?(@.pipeType == 'STEEL' && @.systemType == 'DRY')].cFactor")
						.value(contains(100.0)))
				.andExpect(jsonPath("$[?(@.pipeType == 'CPVC')].cFactor").value(contains(150.0)));
	}

	@Test
	void fittingEquivalentLengthsTable_exposesThePlaceholderStateHonestly() throws Exception {
		// Empty maps plus a placeholder tag are how a form learns it must collect an explicit
		// equivalent length instead of offering the lookup.
		mockMvc.perform(get(BASE_URL + "/sprinkler-fitting-equivalent-lengths-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(7)))
				.andExpect(jsonPath("$[?(@.confidence == 'placeholder')]", hasSize(7)))
				.andExpect(jsonPath("$[?(@.fittingType == 'ELBOW_90_STANDARD')].equivalentLengthFtByNominalSize")
						.value(contains(org.hamcrest.Matchers.anEmptyMap())));
	}

	@Test
	void kFactorsTable_returnsNominalKFactors() throws Exception {
		mockMvc.perform(get(BASE_URL + "/sprinkler-k-factors-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(10)))
				.andExpect(jsonPath("$[?(@.nominalK == 5.6)].orificeNominal").value(contains("1/2 in.")));
	}

}
