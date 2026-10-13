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
				.andExpect(jsonPath("$[?(@.confidence == 'verified')]", hasSize(5)));
	}

	@Test
	void cFactorsTable_returnsThePublishedSet() throws Exception {
		// NFPA 13 2019 Table 27.2.4.8.1 prints ten rows. This file splits the combined
		// "copper tube, brass or stainless steel" row into three, and folds black and galvanized
		// steel together since the table gives them identical values.
		mockMvc.perform(get(BASE_URL + "/sprinkler-c-factors-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(11)))
				.andExpect(jsonPath("$[?(@.pipeType == 'STEEL' && @.systemType == 'WET')].cFactor")
						.value(contains(120.0)))
				.andExpect(jsonPath("$[?(@.pipeType == 'STEEL' && @.systemType == 'DRY')].cFactor")
						.value(contains(100.0)))
				.andExpect(jsonPath("$[?(@.pipeType == 'CPVC')].cFactor").value(contains(150.0)));
	}

	@Test
	void cFactorsTable_separatesLinedFromUnlinedIron() throws Exception {
		// The cement lining is what carries the 140; serving only that row would let a form
		// pick 140 for an unlined main and under-predict its friction loss.
		mockMvc.perform(get(BASE_URL + "/sprinkler-c-factors-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.pipeType == 'DUCTILE_IRON_UNDERGROUND')].cFactor")
						.value(contains(140.0)))
				.andExpect(jsonPath("$[?(@.pipeType == 'UNLINED_CAST_OR_DUCTILE_IRON')].cFactor")
						.value(contains(100.0)))
				.andExpect(jsonPath("$[?(@.pipeType == 'STAINLESS_STEEL')].cFactor")
						.value(contains(150.0)));
	}

	@Test
	void fittingEquivalentLengthsTable_returnsThePublishedChart() throws Exception {
		// NFPA 13 2019 Table 27.2.3.1.1: 8 fitting rows, keyed by the same nominal size strings
		// the pipe reference data uses, so a form can look a fitting up per segment size.
		mockMvc.perform(get(BASE_URL + "/sprinkler-fitting-equivalent-lengths-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(8)))
				// Every row has been checked cell-for-cell against the printed table, the vane type
				// flow switch included.
				.andExpect(jsonPath("$[?(@.confidence == 'verified')]", hasSize(8)))
				.andExpect(jsonPath("$[?(@.fittingType == 'VANE_TYPE_FLOW_SWITCH')].confidence")
						.value(contains("verified")))
				.andExpect(jsonPath("$[?(@.fittingType == 'ELBOW_90_STANDARD')].equivalentLengthFtByNominalSize['2']")
						.value(contains(5.0)))
				.andExpect(jsonPath("$[?(@.fittingType == 'TEE_OR_CROSS_FLOW_TURNED_90')].equivalentLengthFtByNominalSize['4']")
						.value(contains(20.0)));
	}

	@Test
	void cFactorsTable_carriesTheAnnexEpsilonFiguresAndIsFullyVerified() throws Exception {
		// Annex Table A.27.2.4.8.2 pairs some C values with an absolute roughness; a form shows
		// it, the calculation ignores it. It covers fewer materials than the normative C table,
		// so a row it omits carries a null epsilon rather than an invented one.
		mockMvc.perform(get(BASE_URL + "/sprinkler-c-factors-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.pipeType == 'STEEL' && @.systemType == 'WET')].epsilonFactorMm")
						.value(contains(0.1)))
				.andExpect(jsonPath("$[?(@.pipeType == 'STAINLESS_STEEL')].epsilonFactorMm")
						.value(contains((Object) null)))
				.andExpect(jsonPath("$[?(@.confidence == 'verified')]", hasSize(11)));
	}

	@Test
	void kFactorsTable_returnsTheFullPublishedTable() throws Exception {
		// Table 7.2.2.1: 13 nominal K-factors with their allowed range, discharge percentage and
		// thread type (a thread size, not an orifice size).
		mockMvc.perform(get(BASE_URL + "/sprinkler-k-factors-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(13)))
				.andExpect(jsonPath("$[?(@.nominalK == 5.6)].threadType").value(contains("1/2 in. (15 mm) NPT")))
				.andExpect(jsonPath("$[?(@.nominalK == 5.6)].percentOfK56Discharge").value(contains(100.0)))
				.andExpect(jsonPath("$[?(@.nominalK == 28.0)].kFactorRangeMaxGpm").value(contains(29.4)));
	}

}
