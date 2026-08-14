package com.renzoproject.calc_api.mechanical.reference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PipeReferenceControllerTest {

	private static final String URL = "/api/mechanical/reference/pipe-materials";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void pipeMaterials_returns200WithAllFourMaterials() throws Exception {
		mockMvc.perform(get(URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[*].material", hasItems("GI", "BI", "UPVC", "PPR")));
	}

	@Test
	void pipeMaterials_giHasVerifiedConfidenceAndSch40Sizes() throws Exception {
		mockMvc.perform(get(URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].material").value("GI"))
				.andExpect(jsonPath("$[0].confidence").value("verified"))
				.andExpect(jsonPath("$[0].schedules[0].schedule").value("SCH40"))
				.andExpect(jsonPath("$[0].schedules[0].sizes").isNotEmpty());
	}

	@Test
	void pipeMaterials_upvcAndPprHavePlaceholderConfidence() throws Exception {
		mockMvc.perform(get(URL))
				.andExpect(jsonPath("$[2].material").value("UPVC"))
				.andExpect(jsonPath("$[2].confidence").value("placeholder"))
				.andExpect(jsonPath("$[3].material").value("PPR"))
				.andExpect(jsonPath("$[3].confidence").value("placeholder"));
	}

}
