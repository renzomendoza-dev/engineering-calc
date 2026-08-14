package com.renzoproject.calc_api.electrical.voltagedrop;

import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VoltageDropControllerTest {

	private static final String URL = "/api/electrical/voltage-drop";
	private static final double DELTA = 1e-6;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void customImpedance_returns200WithCalculatedValues() throws Exception {
		VoltageDropRequest request = new VoltageDropRequest();
		request.setCircuitType(CircuitType.SINGLE_PHASE_AC);
		request.setCurrentAmps(20.0);
		request.setOneWayLengthMeters(75.0);
		request.setPowerFactor(0.9);
		request.setSystemVoltage(230.0);
		request.setParallelSetsPerPhase(1);
		request.setUseCustomImpedance(true);
		request.setCustomResistanceOhmsPerMeter(0.002);
		request.setCustomReactanceOhmsPerMeter(0.0008);

		double sinTheta = Math.sqrt(1 - 0.9 * 0.9);
		double expectedTerm = (0.002 * 0.9) + (0.0008 * sinTheta);
		double expectedVdrop = 2 * 75.0 * 20.0 * expectedTerm;

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		VoltageDropResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), VoltageDropResponse.class);

		assertEquals(expectedVdrop, response.voltageDropVolts(), DELTA);
		assertEquals(expectedVdrop / 230.0 * 100.0, response.voltageDropPercent(), DELTA);
		assertEquals(230.0 - expectedVdrop, response.receivingEndVoltage(), DELTA);
		assertTrue(response.voltageDropPercent() < 3.0);
	}

	@Test
	void referenceTableImpedance_returns200WithResolvedCalculatedValues() throws Exception {
		// size "100", copper, PVC conduit: r = 0.062/305, x = 0.041/305 ohms/m (PEC Table 10.1.1.9)
		VoltageDropRequest request = new VoltageDropRequest();
		request.setCircuitType(CircuitType.SINGLE_PHASE_AC);
		request.setCurrentAmps(20.0);
		request.setOneWayLengthMeters(75.0);
		request.setPowerFactor(0.9);
		request.setSystemVoltage(230.0);
		request.setParallelSetsPerPhase(1);
		request.setUseCustomImpedance(false);
		request.setConductorSizeLabel("100");
		request.setConductorMaterial(ConductorMaterial.COPPER);
		request.setConduitMaterial(ConduitMaterial.PVC);

		double r = 0.062 / 305.0;
		double x = 0.041 / 305.0;
		double sinTheta = Math.sqrt(1 - 0.9 * 0.9);
		double expectedTerm = (r * 0.9) + (x * sinTheta);
		double expectedVdrop = 2 * 75.0 * 20.0 * expectedTerm;

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		VoltageDropResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), VoltageDropResponse.class);

		assertEquals(expectedVdrop, response.voltageDropVolts(), DELTA);
		assertEquals(expectedVdrop / 230.0 * 100.0, response.voltageDropPercent(), DELTA);
	}

	@Test
	void negativeCurrent_returns400() throws Exception {
		VoltageDropRequest request = new VoltageDropRequest();
		request.setCircuitType(CircuitType.SINGLE_PHASE_AC);
		request.setCurrentAmps(-5.0);
		request.setOneWayLengthMeters(50.0);
		request.setPowerFactor(0.9);
		request.setSystemVoltage(230.0);
		request.setParallelSetsPerPhase(1);
		request.setUseCustomImpedance(true);
		request.setCustomResistanceOhmsPerMeter(0.001);
		request.setCustomReactanceOhmsPerMeter(0.0004);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void requestExceedingRecommendedLimit_returns200WithFlagTrue() throws Exception {
		// pf = 1.0 => sinTheta = 0, term = r = 0.001
		// Vdrop = 2 * 150m * 50A * 0.001 = 15.0V, 15/120*100 = 12.5%
		VoltageDropRequest request = new VoltageDropRequest();
		request.setCircuitType(CircuitType.SINGLE_PHASE_AC);
		request.setCurrentAmps(50.0);
		request.setOneWayLengthMeters(150.0);
		request.setPowerFactor(1.0);
		request.setSystemVoltage(120.0);
		request.setParallelSetsPerPhase(1);
		request.setUseCustomImpedance(true);
		request.setCustomResistanceOhmsPerMeter(0.001);
		request.setCustomReactanceOhmsPerMeter(0.0005);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn();

		VoltageDropResponse response = objectMapper.readValue(
				mvcResult.getResponse().getContentAsString(), VoltageDropResponse.class);

		assertEquals(15.0, response.voltageDropVolts(), DELTA);
		assertEquals(12.5, response.voltageDropPercent(), DELTA);
		assertTrue(response.exceedsRecommendedLimit());
	}

	@Test
	void referenceMode_missingConductorSizeLabel_returns400() throws Exception {
		VoltageDropRequest request = new VoltageDropRequest();
		request.setCircuitType(CircuitType.SINGLE_PHASE_AC);
		request.setCurrentAmps(20.0);
		request.setOneWayLengthMeters(75.0);
		request.setPowerFactor(0.9);
		request.setSystemVoltage(230.0);
		request.setParallelSetsPerPhase(1);
		request.setUseCustomImpedance(false);
		// conductorSizeLabel intentionally left null
		request.setConductorMaterial(ConductorMaterial.COPPER);
		request.setConduitMaterial(ConduitMaterial.PVC);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void customMode_missingCustomResistance_returns400() throws Exception {
		VoltageDropRequest request = new VoltageDropRequest();
		request.setCircuitType(CircuitType.SINGLE_PHASE_AC);
		request.setCurrentAmps(20.0);
		request.setOneWayLengthMeters(75.0);
		request.setPowerFactor(0.9);
		request.setSystemVoltage(230.0);
		request.setParallelSetsPerPhase(1);
		request.setUseCustomImpedance(true);
		// customResistanceOhmsPerMeter intentionally left null
		request.setCustomReactanceOhmsPerMeter(0.0008);

		mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknownConductorSizeLabel_returns400WithCalculationExceptionMessage() throws Exception {
		VoltageDropRequest request = new VoltageDropRequest();
		request.setCircuitType(CircuitType.SINGLE_PHASE_AC);
		request.setCurrentAmps(20.0);
		request.setOneWayLengthMeters(75.0);
		request.setPowerFactor(0.9);
		request.setSystemVoltage(230.0);
		request.setParallelSetsPerPhase(1);
		request.setUseCustomImpedance(false);
		request.setConductorSizeLabel("does-not-exist");
		request.setConductorMaterial(ConductorMaterial.COPPER);
		request.setConduitMaterial(ConduitMaterial.PVC);

		MvcResult mvcResult = mockMvc.perform(post(URL)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andReturn();

		String body = mvcResult.getResponse().getContentAsString();
		assertTrue(body.contains("Unknown conductor size: does-not-exist"));
	}

}
