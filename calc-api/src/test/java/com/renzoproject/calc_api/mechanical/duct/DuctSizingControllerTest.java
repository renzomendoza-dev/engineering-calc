package com.renzoproject.calc_api.mechanical.duct;

import com.renzoproject.calc_api.mechanical.pipe.FrictionFactorMethodDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DuctSizingControllerTest {

	private static final String URL = "/api/mechanical/duct/sizing";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static DuctSizingRequest roundVelocity() {
		return new DuctSizingRequest(
				DuctSizingMethodDto.VELOCITY, 500.0, 20.0, 0.0, "GALVANIZED_STEEL_SPIRAL", FrictionFactorMethodDto.SWAMEE_JAIN,
				DuctShapeDto.ROUND, null, null, null, 5.0);
	}

	private static DuctSizingRequest roundEqualFriction(FrictionFactorMethodDto method) {
		return new DuctSizingRequest(
				DuctSizingMethodDto.EQUAL_FRICTION, 500.0, 20.0, 0.0, "GALVANIZED_STEEL_SPIRAL", method,
				DuctShapeDto.ROUND, null, null, 1.0, null);
	}

	@Test
	void round_velocity_validRequest_returns200() throws Exception {
		DuctSizingResponse response = postForResponse(roundVelocity());

		assertNotNull(response.equivalentDiameterMm());
		assertNull(response.ductWidthMm());
		assertNull(response.ductHeightMm());
		assertTrue(response.actualVelocityMps() > 0);
	}

	@Test
	void round_equalFriction_swameeJain_validRequest_returns200() throws Exception {
		DuctSizingResponse response = postForResponse(roundEqualFriction(FrictionFactorMethodDto.SWAMEE_JAIN));

		assertNotNull(response.equivalentDiameterMm());
		assertTrue(response.actualFrictionRatePaPerM() > 0);
	}

	@Test
	void round_equalFriction_colebrookWhite_validRequest_returns200() throws Exception {
		DuctSizingResponse response = postForResponse(roundEqualFriction(FrictionFactorMethodDto.COLEBROOK_WHITE));

		assertNotNull(response.equivalentDiameterMm());
		assertTrue(response.actualFrictionRatePaPerM() > 0);
	}

	@Test
	void rectangular_velocity_fixedHeight_validRequest_returns200() throws Exception {
		DuctSizingRequest request = new DuctSizingRequest(
				DuctSizingMethodDto.VELOCITY, 500.0, 20.0, 0.0, "GALVANIZED_STEEL_SPIRAL", FrictionFactorMethodDto.SWAMEE_JAIN,
				DuctShapeDto.RECTANGULAR, FixedDimensionTypeDto.HEIGHT, 300.0, null, 5.0);

		DuctSizingResponse response = postForResponse(request);

		assertNotNull(response.ductWidthMm());
		assertEquals(300.0, response.ductHeightMm(), 1e-6);
	}

	@Test
	void rectangular_equalFriction_fixedHeight_validRequest_returns200() throws Exception {
		DuctSizingRequest request = new DuctSizingRequest(
				DuctSizingMethodDto.EQUAL_FRICTION, 500.0, 20.0, 0.0, "GALVANIZED_STEEL_SPIRAL", FrictionFactorMethodDto.SWAMEE_JAIN,
				DuctShapeDto.RECTANGULAR, FixedDimensionTypeDto.HEIGHT, 300.0, 1.0, null);

		DuctSizingResponse response = postForResponse(request);

		assertNotNull(response.ductWidthMm());
		assertEquals(300.0, response.ductHeightMm(), 1e-6);
	}

	@Test
	void rectangular_missingFixedDimensionType_returns400() throws Exception {
		DuctSizingRequest request = new DuctSizingRequest(
				DuctSizingMethodDto.VELOCITY, 500.0, 20.0, 0.0, "GALVANIZED_STEEL_SPIRAL", FrictionFactorMethodDto.SWAMEE_JAIN,
				DuctShapeDto.RECTANGULAR, null, 300.0, null, 5.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rectangular_missingFixedDimensionValue_returns400() throws Exception {
		DuctSizingRequest request = new DuctSizingRequest(
				DuctSizingMethodDto.VELOCITY, 500.0, 20.0, 0.0, "GALVANIZED_STEEL_SPIRAL", FrictionFactorMethodDto.SWAMEE_JAIN,
				DuctShapeDto.RECTANGULAR, FixedDimensionTypeDto.HEIGHT, null, null, 5.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void equalFriction_missingTargetFrictionRate_returns400() throws Exception {
		DuctSizingRequest request = new DuctSizingRequest(
				DuctSizingMethodDto.EQUAL_FRICTION, 500.0, 20.0, 0.0, "GALVANIZED_STEEL_SPIRAL", FrictionFactorMethodDto.SWAMEE_JAIN,
				DuctShapeDto.ROUND, null, null, null, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void velocity_missingMaxVelocity_returns400() throws Exception {
		DuctSizingRequest request = new DuctSizingRequest(
				DuctSizingMethodDto.VELOCITY, 500.0, 20.0, 0.0, "GALVANIZED_STEEL_SPIRAL", FrictionFactorMethodDto.SWAMEE_JAIN,
				DuctShapeDto.ROUND, null, null, null, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknownDuctMaterial_isCalcCoreValidation_returns400() throws Exception {
		DuctSizingRequest request = new DuctSizingRequest(
				DuctSizingMethodDto.VELOCITY, 500.0, 20.0, 0.0, "NOT_A_REAL_MATERIAL", FrictionFactorMethodDto.SWAMEE_JAIN,
				DuctShapeDto.ROUND, null, null, null, 5.0);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nonConvergentEqualFriction_isCalcCoreValidation_returns400() throws Exception {
		// Absurdly low target friction rate implies an enormous diameter, outside the
		// bisection solver's bracket -- must surface as 400, not a 500 or a silent bad answer.
		DuctSizingRequest request = new DuctSizingRequest(
				DuctSizingMethodDto.EQUAL_FRICTION, 500.0, 20.0, 0.0, "GALVANIZED_STEEL_SPIRAL", FrictionFactorMethodDto.SWAMEE_JAIN,
				DuctShapeDto.ROUND, null, null, 1e-12, null);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	private DuctSizingResponse postForResponse(DuctSizingRequest request) throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DuctSizingResponse.class);
	}

}
