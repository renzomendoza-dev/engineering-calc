package com.renzoproject.calc_api.electrical.wiresizing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WireSizingControllerTest {

	private static final String URL = "/api/electrical/wire-sizing";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void validRequestWithoutVoltageDropCheck_matchesHandVerifiedRecommendation() throws Exception {
		// THHN copper -> 90C column. 30C ambient, 90C factor = 1.0. count=2 -> no adjustment.
		// COPPER "2.0" @ 90C = 25A >= 15A required -> recommended immediately.
		WireSizingRequest request = new WireSizingRequest(
				15.0, false, 30.0, 2, 1, "THHN", "COPPER", 75, null, null, null, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		WireSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), WireSizingResponse.class);

		assertEquals("2.0", response.recommendedSizeLabel());
		assertEquals(25.0, response.baseAmpacityAmps(), DELTA);
		assertEquals(1.0, response.tempCorrectionFactor(), DELTA);
		assertEquals(1.0, response.adjustmentFactor(), DELTA);
		assertEquals(25.0, response.deratedAmpacityAmps(), DELTA);
		assertEquals(15.0, response.requiredAmpacityAmps(), DELTA);
		assertNull(response.voltageDropCheckResult());
	}

	private WireSizingResponse postForResponse(WireSizingRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), WireSizingResponse.class);
	}

	@Test
	void withoutAnOvercurrentDevice_theSmallConductorLimitIsStillReported() throws Exception {
		// 20 A of THHN copper fits 2.0 mm2 by ampacity; PEC 2.40.1.4(D)(3) caps its protection at 15 A.
		WireSizingResponse response = postForResponse(new WireSizingRequest(20.0, false, 30.0, 2, 1, "THHN", "COPPER", 75, null, null, null, null));

		assertEquals("2.0", response.recommendedSizeLabel());
		assertEquals(15, response.maximumOvercurrentDeviceAmps());
		assertNull(response.overcurrentDeviceAmps());
		assertFalse(response.sizeRaisedForOvercurrentLimit());
	}

	@Test
	void aDeviceAboveTheSmallConductorLimit_raisesTheRecommendedSize() throws Exception {
		WireSizingResponse response = postForResponse(new WireSizingRequest(20.0, false, 30.0, 2, 1, "THHN", "COPPER", 75, null, 20.0, null, null));

		assertEquals("3.5", response.recommendedSizeLabel());
		assertEquals("2.0", response.ampacitySizeLabel());
		assertTrue(response.sizeRaisedForOvercurrentLimit());
		assertEquals(20, response.maximumOvercurrentDeviceAmps());
		assertEquals(20.0, response.overcurrentDeviceAmps(), DELTA);
	}

	@Test
	void aNipple_takesNoConductorCountAdjustment() throws Exception {
		// 24 A on seven conductors: 5.5 mm2 with the 70 % factor as a run, 2.0 mm2 as a nipple (PEC Chapter 10 note 4).
		WireSizingResponse run = postForResponse(new WireSizingRequest(24.0, false, 30.0, 7, 1, "THHN", "COPPER", 75, null, null, false, null));
		WireSizingResponse nipple = postForResponse(new WireSizingRequest(24.0, false, 30.0, 7, 1, "THHN", "COPPER", 75, null, null, true, null));

		assertEquals("5.5", run.recommendedSizeLabel());
		assertEquals(0.7, run.adjustmentFactor(), DELTA);
		assertFalse(run.nipple());
		assertEquals("2.0", nipple.recommendedSizeLabel());
		assertEquals(1.0, nipple.adjustmentFactor(), DELTA);
		assertTrue(nipple.nipple());
	}

	@Test
	void aDeviceAboveTheNextStandardRating_raisesTheSize() throws Exception {
		// 45 A: 8.0 mm2 carries 55 A and may take a 60 A device (2.40.1.4(B)); 70 A needs 14 mm2.
		WireSizingResponse sixty = postForResponse(new WireSizingRequest(45.0, false, 30.0, 3, 1, "THHN", "COPPER", 75, null, 60.0, null, null));
		WireSizingResponse seventy = postForResponse(new WireSizingRequest(45.0, false, 30.0, 3, 1, "THHN", "COPPER", 75, null, 70.0, null, null));
		WireSizingResponse receptacles = postForResponse(new WireSizingRequest(45.0, false, 30.0, 3, 1, "THHN", "COPPER", 75, null, 60.0, null, true));

		assertEquals("8.0", sixty.recommendedSizeLabel());
		assertEquals(60, sixty.standardRatingLimitAmps());
		assertNull(sixty.overcurrentLimitClause());
		assertEquals("14", seventy.recommendedSizeLabel());
		assertEquals("2.40.1.4(B)", seventy.overcurrentLimitClause());
		assertEquals("14", receptacles.recommendedSizeLabel());
		assertTrue(receptacles.multipleReceptacleBranchCircuit());
	}

	@Test
	void aRequestWithoutTheNippleField_isARun() throws Exception {
		String json = """
				{"loadCurrentAmps": 24, "isContinuousLoad": false, "ambientTempCelsius": 30,
				 "numberOfCurrentCarryingConductors": 7, "numberOfParallelSets": 1, "insulationType": "THHN",
				 "conductorMaterial": "COPPER", "terminationTempRatingCelsius": 75}""";

		MvcResult mvcResult = mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andReturn();
		WireSizingResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), WireSizingResponse.class);

		assertFalse(response.nipple());
		assertEquals("5.5", response.recommendedSizeLabel());
	}

	@Test
	void aNonPositiveOvercurrentDevice_returns400() throws Exception {
		WireSizingRequest request = new WireSizingRequest(20.0, false, 30.0, 2, 1, "THHN", "COPPER", 75, null, 0.0, null, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void voltageDropCheck_ampacityBasedSizeAlsoPasses_upsizedRecommendationNull() throws Exception {
		WireSizingRequest request = new WireSizingRequest(
				15.0, false, 30.0, 2, 1, "THHN", "COPPER", 75,
				new VoltageDropCheckRequestDto("SINGLE_PHASE_AC", 5.0, 0.9, 230.0, "PVC", 1), null, null, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		WireSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), WireSizingResponse.class);

		assertEquals("2.0", response.recommendedSizeLabel());
		assertNotNull(response.voltageDropCheckResult());
		assertEquals("2.0", response.voltageDropCheckResult().sizeLabelChecked());
		assertFalse(response.voltageDropCheckResult().exceedsRecommendedLimit());
		assertNull(response.voltageDropCheckResult().upsizedRecommendation());
	}

	@Test
	void voltageDropCheck_ampacityBasedSizeFails_upsizedRecommendationPopulated() throws Exception {
		// Same ampacity scenario, but a 200m run pushes voltage drop at "2.0" far past 3%.
		WireSizingRequest request = new WireSizingRequest(
				15.0, false, 30.0, 2, 1, "THHN", "COPPER", 75,
				new VoltageDropCheckRequestDto("SINGLE_PHASE_AC", 200.0, 0.9, 230.0, "PVC", 1), null, null, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		WireSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), WireSizingResponse.class);

		assertEquals("2.0", response.recommendedSizeLabel());
		assertNotNull(response.voltageDropCheckResult());
		assertTrue(response.voltageDropCheckResult().exceedsRecommendedLimit());
		assertNotNull(response.voltageDropCheckResult().upsizedRecommendation());
	}

	@Test
	void unknownInsulationType_returns400() throws Exception {
		WireSizingRequest request = new WireSizingRequest(
				15.0, false, 30.0, 2, 1, "NOT_REAL", "COPPER", 75, null, null, null, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknownConductorMaterial_returns400() throws Exception {
		WireSizingRequest request = new WireSizingRequest(
				15.0, false, 30.0, 2, 1, "THHN", "NOT_REAL", 75, null, null, null, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void noSizeSatisfiesRequiredAmpacity_returns400WithErrorMessage() throws Exception {
		// 10,000A is far beyond even the largest published size (500 COPPER @ 90C = 595A).
		WireSizingRequest request = new WireSizingRequest(
				10000.0, false, 30.0, 1, 1, "THHN", "COPPER", 75, null, null, null, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andReturn();

		String body = mvcResult.getResponse().getContentAsString();
		assertTrue(body.contains("No conductor size in the table satisfies the required ampacity"));
	}

	@Test
	void parallelSets_splitsLoadAcrossConductors_recommendsSmallerPerConductorSize() throws Exception {
		// Same worked example as WireSizingCalculatorTest's parallel-sets test: 1000A total,
		// 90C COPPER column, 2 sets -> each conductor only needs 500A -> "375" (530A @ 90C).
		WireSizingRequest request = new WireSizingRequest(
				1000.0, false, 30.0, 2, 2, "THHN", "COPPER", 90, null, null, null, null);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		WireSizingResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), WireSizingResponse.class);

		assertEquals("375", response.recommendedSizeLabel());
		assertEquals(2, response.numberOfParallelSets());
		assertEquals(500.0, response.requiredAmpacityPerSetAmps(), DELTA);
		assertEquals(1000.0, response.requiredAmpacityAmps(), DELTA);
	}

}
