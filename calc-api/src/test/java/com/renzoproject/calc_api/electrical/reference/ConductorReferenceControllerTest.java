package com.renzoproject.calc_api.electrical.reference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConductorReferenceControllerTest {

	private static final String BASE_URL = "/api/electrical/reference";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void conductorSizes_returns200WithNonEmptyList() throws Exception {
		mockMvc.perform(get(BASE_URL + "/conductor-sizes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$[0].label").exists())
				.andExpect(jsonPath("$[0].crossSectionMm2").exists());
	}

	@Test
	void conductorMaterials_returns200WithCopperAndAluminum() throws Exception {
		mockMvc.perform(get(BASE_URL + "/conductor-materials"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasItems("COPPER", "ALUMINUM")));
	}

	@Test
	void conduitMaterials_returns200WithPvcAluminumSteel() throws Exception {
		mockMvc.perform(get(BASE_URL + "/conduit-materials"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasItems("PVC", "ALUMINUM", "STEEL")));
	}

	@Test
	void insulationTypes_returns200WithNonEmptyList() throws Exception {
		mockMvc.perform(get(BASE_URL + "/insulation-types"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem("THHN")));
	}

	@Test
	void insulationTypes_includesTheEightWireSizingTypes() throws Exception {
		// listInsulationTypes() reads InsulationType.values() dynamically, not a hardcoded
		// list, so it should already reflect the 8 constants added in the wire sizing session
		// without any change here.
		mockMvc.perform(get(BASE_URL + "/insulation-types"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasItems(
						"MI", "SA", "SIS", "TBS", "UF", "USE", "USE-2", "ZW-2")));
	}

	@Test
	void terminationTempRatings_returnsExactlySixtySeventyFiveNinety() throws Exception {
		mockMvc.perform(get(BASE_URL + "/termination-temp-ratings"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.contains(60, 75, 90)));
	}

	@Test
	void conduitFillTypes_returns200WithNonEmptyList() throws Exception {
		mockMvc.perform(get(BASE_URL + "/conduit-fill-types"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isNotEmpty())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem("EMT")));
	}

	@Test
	void conductorFillSizes_forThhn_returns200WithExpectedSizes() throws Exception {
		mockMvc.perform(get(BASE_URL + "/conductor-fill-sizes").param("insulationType", "THHN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasItems("100", "14")));
	}

	@Test
	void conductorFillSizes_unknownInsulationType_returns400() throws Exception {
		mockMvc.perform(get(BASE_URL + "/conductor-fill-sizes").param("insulationType", "NOT_REAL"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void motorPhaseTypes_returns200WithAllFourValues() throws Exception {
		mockMvc.perform(get(BASE_URL + "/motor-phase-types"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.containsInAnyOrder(
						"DC", "SINGLE_PHASE", "TWO_PHASE", "THREE_PHASE")));
	}

	@Test
	void motorClasses_returns200WithInductionAndSynchronous() throws Exception {
		mockMvc.perform(get(BASE_URL + "/motor-classes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.containsInAnyOrder("INDUCTION", "SYNCHRONOUS")));
	}

	@Test
	void motorHorsepowerRatings_threePhaseInduction_returns200WithTenHp() throws Exception {
		mockMvc.perform(get(BASE_URL + "/motor-horsepower-ratings")
						.param("phaseType", "THREE_PHASE")
						.param("motorClass", "INDUCTION"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem("10")));
	}

	@Test
	void motorHorsepowerRatings_singlePhaseWithoutMotorClass_returns200() throws Exception {
		mockMvc.perform(get(BASE_URL + "/motor-horsepower-ratings")
						.param("phaseType", "SINGLE_PHASE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem("5")));
	}

	@Test
	void motorHorsepowerRatings_unknownPhaseType_returns400() throws Exception {
		mockMvc.perform(get(BASE_URL + "/motor-horsepower-ratings")
						.param("phaseType", "NOT_REAL"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void motorHorsepowerRatings_threePhaseWithoutMotorClass_returns400() throws Exception {
		mockMvc.perform(get(BASE_URL + "/motor-horsepower-ratings")
						.param("phaseType", "THREE_PHASE"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void motorHorsepowerRatings_threePhaseWithUnknownMotorClass_returns400() throws Exception {
		mockMvc.perform(get(BASE_URL + "/motor-horsepower-ratings")
						.param("phaseType", "THREE_PHASE")
						.param("motorClass", "NOT_REAL"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void motorVoltages_threePhaseInductionTenHp_returns200With230() throws Exception {
		mockMvc.perform(get(BASE_URL + "/motor-voltages")
						.param("phaseType", "THREE_PHASE")
						.param("motorClass", "INDUCTION")
						.param("horsepowerLabel", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem(230)));
	}

	@Test
	void motorVoltages_threePhaseWithoutMotorClass_returns400() throws Exception {
		mockMvc.perform(get(BASE_URL + "/motor-voltages")
						.param("phaseType", "THREE_PHASE")
						.param("horsepowerLabel", "10"))
				.andExpect(status().isBadRequest());
	}

}
