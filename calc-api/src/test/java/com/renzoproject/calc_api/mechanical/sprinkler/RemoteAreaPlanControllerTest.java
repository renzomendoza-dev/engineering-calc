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

	/** Light hazard design basis: 1500 ft2, 0.10 gpm/ft2, K 5.6, 100 ft2 per sprinkler. */
	private static String body(String extra) {
		return "{\"designAreaSqFt\":1500,\"densityGpmPerSqFt\":0.10,\"kFactor\":5.6,"
				+ "\"coveragePerSprinklerSqFt\":100," + extra + "}";
	}

	@Test
	void withTheSpacing_returnsTheFullHandWorkedLayoutAndTheFlooredFirstSprinkler() throws Exception {
		// 1.2 * sqrt(1500) = 46.476 ft -> 5 sprinklers per branch; 1500 / (5 * 100) = 3 branches;
		// 15 sprinklers protecting 1500 ft2. 10 gpm needs (10/5.6)^2 = 3.19 psi, under 7, so 7 psi
		// governs and the sprinkler discharges 5.6 * sqrt(7) = 14.816 gpm.
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(body("\"sprinklerSpacingFt\":10,\"quickResponseSprinklers\":false")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.adjustedDesignAreaSqFt").value(1500.0))
				.andExpect(jsonPath("$.sprinklersForArea").value(15))
				.andExpect(jsonPath("$.flowPerSprinklerGpm").value(10.0))
				.andExpect(jsonPath("$.remoteAreaLengthFt").value(closeTo(46.4758, 1e-3)))
				.andExpect(jsonPath("$.branchSpacingFt").value(10.0))
				.andExpect(jsonPath("$.sprinklersPerBranch").value(5))
				.andExpect(jsonPath("$.branchLines").value(3))
				.andExpect(jsonPath("$.totalSprinklers").value(15))
				.andExpect(jsonPath("$.totalProtectedAreaSqFt").value(1500.0))
				.andExpect(jsonPath("$.pressureForFlowPsi").value(closeTo(3.1888, 1e-3)))
				.andExpect(jsonPath("$.floorGoverned").value(true))
				.andExpect(jsonPath("$.firstSprinklerPressurePsi").value(7.0))
				.andExpect(jsonPath("$.firstSprinklerFlowGpm").value(closeTo(14.8162, 1e-4)));
	}

	@Test
	void withoutTheSpacing_returnsTheDesignBasisFiguresAndNullLayout() throws Exception {
		// What step 1 of a form can show before any layout is known.
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(body("\"quickResponseSprinklers\":false")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sprinklersForArea").value(15))
				.andExpect(jsonPath("$.flowPerSprinklerGpm").value(10.0))
				.andExpect(jsonPath("$.firstSprinklerPressurePsi").value(7.0))
				.andExpect(jsonPath("$.remoteAreaLengthFt").value(closeTo(46.4758, 1e-3)))
				.andExpect(jsonPath("$.branchSpacingFt").doesNotExist())
				.andExpect(jsonPath("$.sprinklersPerBranch").doesNotExist())
				.andExpect(jsonPath("$.branchLines").doesNotExist())
				.andExpect(jsonPath("$.totalSprinklers").doesNotExist())
				.andExpect(jsonPath("$.totalProtectedAreaSqFt").doesNotExist());
	}

	@Test
	void quickResponseAtFifteenFeet_reducesTheAreaBeforeCountingAndLayingItOut() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(body("\"sprinklerSpacingFt\":10,\"quickResponseSprinklers\":true,"
								+ "\"ceilingHeightFt\":15")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reductionApplied").value(true))
				.andExpect(jsonPath("$.reductionPercent").value(32.5))
				.andExpect(jsonPath("$.adjustedDesignAreaSqFt").value(1012.5))
				.andExpect(jsonPath("$.sprinklersForArea").value(11))
				.andExpect(jsonPath("$.sprinklersPerBranch").value(4))
				.andExpect(jsonPath("$.branchLines").value(3))
				.andExpect(jsonPath("$.totalSprinklers").value(12))
				.andExpect(jsonPath("$.notes[*]").value(hasItem(containsString("not checked here"))));
	}

	@Test
	void workbookFrontPage_matchesTheSheet() throws Exception {
		// Ordinary hazard 0.15 over 130 ft2, K 5.6: 12 sprinklers, 19.5 gpm, 12.125 psi (not floored).
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content("{\"designAreaSqFt\":1500,\"densityGpmPerSqFt\":0.15,\"kFactor\":5.6,"
								+ "\"coveragePerSprinklerSqFt\":130,\"quickResponseSprinklers\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sprinklersForArea").value(12))
				.andExpect(jsonPath("$.flowPerSprinklerGpm").value(19.5))
				.andExpect(jsonPath("$.pressureForFlowPsi").value(closeTo(12.1253, 1e-3)))
				.andExpect(jsonPath("$.floorGoverned").value(false))
				.andExpect(jsonPath("$.firstSprinklerFlowGpm").value(19.5));
	}

	@Test
	void confirmedLayoutRules_areNotFlagged() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(body("\"quickResponseSprinklers\":false")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rulesConfidence").value("verified"))
				.andExpect(jsonPath("$.warnings").value(org.hamcrest.Matchers.not(
						hasItem(containsString("reference data")))));
	}

	@Test
	void coverageAboveTheHazardMaximum_isWarnedAbout() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content("{\"designAreaSqFt\":1500,\"densityGpmPerSqFt\":0.15,\"kFactor\":5.6,"
								+ "\"coveragePerSprinklerSqFt\":150,\"quickResponseSprinklers\":false,"
								+ "\"maxCoveragePerSprinklerSqFt\":130}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.warnings[*]").value(hasItem(containsString("above the 130 ft2 maximum"))));
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
				.andExpect(jsonPath("$.fieldErrors.coveragePerSprinklerSqFt").exists());
	}

	@Test
	void nonPositiveCoverage_isBeanValidation_returns400() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content("{\"designAreaSqFt\":1500,\"densityGpmPerSqFt\":0.1,\"kFactor\":5.6,"
								+ "\"coveragePerSprinklerSqFt\":0,\"quickResponseSprinklers\":false}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.coveragePerSprinklerSqFt").exists());
	}

	@Test
	void aSpacingOfZero_whenGiven_isBeanValidation_returns400() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(body("\"sprinklerSpacingFt\":0,\"quickResponseSprinklers\":false")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.sprinklerSpacingFt").exists());
	}

	@Test
	void aListedMinimumPressure_raisesTheFloorAndIsNamedInTheNotes() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content(body("\"quickResponseSprinklers\":false,\"listedMinimumPressurePsi\":10")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.minimumOperatingPressurePsi").value(10.0))
				.andExpect(jsonPath("$.firstSprinklerPressurePsi").value(10.0))
				.andExpect(jsonPath("$.notes").value(hasItem(containsString("listing's minimum operating pressure of 10 psi"))));
	}
}
