package com.renzoproject.calc_api.smokecontrol.naturalventing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NaturalVentingReferenceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void reference_servesTheTwoTablesWithTheirEdition() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/smoke-control/natural-venting/reference")).andExpect(status().isOk()).andReturn();
		NaturalVentingReferenceResponse reference = objectMapper.readValue(result.getResponse().getContentAsString(),
				NaturalVentingReferenceResponse.class);

		assertTrue(reference.edition().contains("2021"));
		assertEquals(6, reference.dischargeCoefficients().size());
		assertEquals(0.25, reference.dischargeCoefficients().stream().filter(row -> row.key().equals("WEATHER_LOUVER_45")).findFirst().orElseThrow()
				.dischargeCoefficient(), 1e-12);
		assertEquals(23, reference.commodities().size());
		assertEquals(10200.0, reference.commodities().stream().filter(row -> row.key().equals("WOOD_PALLETS_4_88")).findFirst().orElseThrow()
				.heatReleaseRateKwPerM2(), 1e-9);
	}

}
