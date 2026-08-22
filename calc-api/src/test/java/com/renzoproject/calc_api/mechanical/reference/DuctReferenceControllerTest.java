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
 * Confirms {@code /api/mechanical/reference/duct-velocity-limits-table} and
 * {@code /duct-roughness-table} return the full, correctly-mapped contents of their
 * respective reference/duct/*.json files.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DuctReferenceControllerTest {

	private static final String BASE_URL = "/api/mechanical/reference";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void ductVelocityLimitsTable_returnsFullReferenceDataSet() throws Exception {
		// Real reference/duct/duct-velocity-limits.json has 9 rows (3 locations x 3 NC/RC ratings).
		mockMvc.perform(get(BASE_URL + "/duct-velocity-limits-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$", hasSize(9)))
				.andExpect(jsonPath("$[?(@.ductLocation == 'WITHIN_OCCUPIED_SPACE' && @.ncRcRating == 35)].maxVelocityRoundMps")
						.value(contains(13.2)))
				.andExpect(jsonPath("$[?(@.ductLocation == 'WITHIN_OCCUPIED_SPACE' && @.ncRcRating == 35)].maxVelocityRectangularMps")
						.value(contains(7.4)))
				.andExpect(jsonPath("$[?(@.ductLocation == 'IN_SHAFT_OR_ABOVE_SOLID_DRYWALL_CEILING')]").exists())
				.andExpect(jsonPath("$[?(@.ductLocation == 'ABOVE_SUSPENDED_ACOUSTICAL_CEILING')]").exists());
	}

	@Test
	void ductRoughnessTable_returnsFullReferenceDataSetWithTwelveMaterials() throws Exception {
		// Real reference/duct/duct-roughness.json has 12 materials.
		mockMvc.perform(get(BASE_URL + "/duct-roughness-table"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$", hasSize(12)))
				.andExpect(jsonPath("$[?(@.material == 'GALVANIZED_STEEL_SPIRAL')].absoluteRoughnessMm")
						.value(contains(0.12)));
	}

	@Test
	void ductRoughnessTable_flaggedMaterialsCarryCaveatSourceNotes() throws Exception {
		// FIBERGLASS_DUCT_RIGID and CONCRETE_DUCT are the two entries with an extra caveat in
		// sourceNote even though the file's overall confidence is "verified" -- the frontend
		// caveat badge keys off this field for exactly these two, not off any per-entry
		// confidence flag (there isn't one).
		mockMvc.perform(get(BASE_URL + "/duct-roughness-table"))
				.andExpect(jsonPath("$[?(@.material == 'FIBERGLASS_DUCT_RIGID')].sourceNote",
						contains(org.hamcrest.Matchers.containsString("tentatively medium rough"))))
				.andExpect(jsonPath("$[?(@.material == 'CONCRETE_DUCT')].sourceNote",
						contains(org.hamcrest.Matchers.containsString("unusually wide"))));
	}

}
