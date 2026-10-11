package com.renzoproject.calc_api.mechanical.sprinkler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RemoteAreaPlanControllerTest {

	private static final String URL = "/api/mechanical/sprinkler/remote-area";

	@Autowired
	private MockMvc mockMvc;

	private static String body(String extra) {
		return "{\"designAreaSqFt\":1500,\"densityGpmPerSqFt\":0.10,\"kFactor\":5.6,"
				+ "\"sprinklerSpacingFt\":10,\"branchSpacingFt\":10," + extra + "}";
	}

	@Test
	void lightHazard_returnsTheHandWorkedLayoutAndTheFlooredFirstSprinkler() throws Exception {
		// 1.2 * sqrt(1500) = 46.476 ft -> 5 sprinklers per branch; 1500 / (5 * 100) = 3 branches;
		// 15 sprinklers protecting 1500 ft2. 10 gpm needs (10/5.6)^2 = 3.19 psi, under 7, so 7 psi
		// governs and the sprinkler discharges 5.6 * sqrt(7) = 14.816 gpm.
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(body("\"quickResponseSprinklers\":false")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.adjustedDesignAreaSqFt").value(1500.0))
				.andExpect(jsonPath("$.remoteAreaLengthFt").value(closeTo(46.4758, 1e-3)))
				.andExpect(jsonPath("$.sprinklersPerBranch").value(5))
				.andExpect(jsonPath("$.branchLines").value(3))
				.andExpect(jsonPath("$.totalSprinklers").value(15))
				.andExpect(jsonPath("$.totalProtectedAreaSqFt").value(1500.0))
				.andExpect(jsonPath("$.pressureForMinimumFlowPsi").value(closeTo(3.1888, 1e-3)))
				.andExpect(jsonPath("$.floorGoverned").value(true))
				.andExpect(jsonPath("$.firstSprinklerPressurePsi").value(7.0))
				.andExpect(jsonPath("$.firstSprinklerFlowGpm").value(closeTo(14.8162, 1e-4)));
	}

	@Test
	void quickResponseAtFifteenFeet_reducesTheAreaBeforeLayingItOut() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(body("\"quickResponseSprinklers\":true,\"ceilingHeightFt\":15")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reductionApplied").value(true))
				.andExpect(jsonPath("$.reductionPercent").value(32.5))
				.andExpect(jsonPath("$.adjustedDesignAreaSqFt").value(1012.5))
				.andExpect(jsonPath("$.sprinklersPerBranch").value(4))
				.andExpect(jsonPath("$.branchLines").value(3))
				.andExpect(jsonPath("$.totalSprinklers").value(12))
				.andExpect(jsonPath("$.notes[*]").value(hasItem(containsString("not checked here"))));
	}

	@Test
	void unverifiedLayoutRules_areDisclosedInTheWarnings() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(body("\"quickResponseSprinklers\":false")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rulesConfidence").value("verify"))
				.andExpect(jsonPath("$.warnings[*]").value(hasItem(containsString("confirm both against"))));
	}

	@Test
	void quickResponseWithoutACeilingHeight_returns400WithTheReason() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(body("\"quickResponseSprinklers\":true")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("ceilingHeightFt is required")));
	}

	@Test
	void missingRequiredField_isBeanValidation_returns400() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content("{\"designAreaSqFt\":1500}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.kFactor").exists())
				.andExpect(jsonPath("$.fieldErrors.sprinklerSpacingFt").exists());
	}

	@Test
	void nonPositiveSpacing_isBeanValidation_returns400() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content("{\"designAreaSqFt\":1500,\"densityGpmPerSqFt\":0.1,\"kFactor\":5.6,"
								+ "\"sprinklerSpacingFt\":0,\"branchSpacingFt\":10,\"quickResponseSprinklers\":false}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.sprinklerSpacingFt").exists());
	}

}
