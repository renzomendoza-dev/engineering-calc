package com.renzoproject.calc_api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the error contract the frontend depends on: every 400, whatever its cause, is
 * {@code {"message": string, "fieldErrors": object}} -- exactly those two keys, never the legacy
 * {@code error} key. Exercised through real endpoints so the whole path (binding, validation,
 * calc-core, advice) is covered, not the handler in isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

	private static final String POWER_CONSUMPTION_URL = "/api/electrical/power-consumption";
	private static final String DUCT_SIZING_URL = "/api/mechanical/duct/sizing";

	@Autowired
	private MockMvc mockMvc;

	private ResultActions postJson(String url, String json) throws Exception {
		return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static void assertEnvelopeShape(ResultActions result) throws Exception {
		result.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$", aMapWithSize(2)))
				.andExpect(jsonPath("$.message").isString())
				.andExpect(jsonPath("$.fieldErrors").isMap())
				.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void calculationException_returnsCalcCoreMessage_withEmptyFieldErrors() throws Exception {
		ResultActions result = postJson(POWER_CONSUMPTION_URL, """
				{"mode": "NOT_A_MODE", "wattageInput": 65.0, "totalOperatingHours": 5.0}
				""");

		assertEnvelopeShape(result);
		result.andExpect(jsonPath("$.message").value("Unknown power input mode: NOT_A_MODE"))
				.andExpect(jsonPath("$.fieldErrors", anEmptyMap()));
	}

	@Test
	void beanValidationFailure_returnsPerFieldMessages() throws Exception {
		// Two violations: @NotBlank mode and @Positive totalOperatingHours.
		ResultActions result = postJson(POWER_CONSUMPTION_URL, """
				{"mode": "", "wattageInput": 65.0, "totalOperatingHours": 0}
				""");

		assertEnvelopeShape(result);
		result.andExpect(jsonPath("$.message").value(GlobalExceptionHandler.VALIDATION_FAILED_MESSAGE))
				.andExpect(jsonPath("$.fieldErrors", aMapWithSize(2)))
				.andExpect(jsonPath("$.fieldErrors.mode").isString())
				.andExpect(jsonPath("$.fieldErrors.totalOperatingHours").isString());
	}

	@Test
	void malformedJson_returnsEnvelope_notSpringDefaultErrorBody() throws Exception {
		ResultActions result = postJson(POWER_CONSUMPTION_URL, "{not valid json");

		assertEnvelopeShape(result);
		result.andExpect(jsonPath("$.message").value(GlobalExceptionHandler.UNREADABLE_BODY_MESSAGE))
				.andExpect(jsonPath("$.fieldErrors", anEmptyMap()))
				// Spring's default error body would carry these; their absence proves the advice ran.
				.andExpect(jsonPath("$.status").doesNotExist())
				.andExpect(jsonPath("$.path").doesNotExist());
	}

	@Test
	void unknownValueForEnumTypedField_returnsEnvelope() throws Exception {
		// DuctSizingRequest.shape binds directly to an enum, so a bad value fails in Jackson before
		// Bean Validation or calc-core ever run.
		ResultActions result = postJson(DUCT_SIZING_URL, """
				{"shape": "TRIANGLE"}
				""");

		assertEnvelopeShape(result);
		result.andExpect(jsonPath("$.message").value(GlobalExceptionHandler.UNREADABLE_BODY_MESSAGE));
	}

}
