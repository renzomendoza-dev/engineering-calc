package com.renzoproject.calc_api.compliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.renzoproject.calc.core.compliance.CheckBasis;
import com.renzoproject.calc.core.compliance.CheckStatus;
import com.renzoproject.calc.core.compliance.ComplianceCheck;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

/** The shared check line keeps each calculator's JSON as it was: only natural venting sends a basis. */
class ComplianceCheckDtoTest {

	private final JsonMapper mapper = JsonMapper.builder().build();

	@Test
	void aCheckWithoutABasis_leavesBasisOutOfTheJson() {
		ComplianceCheck check = new ComplianceCheck("10.2.6.1", "Pressure at the outlet", CheckStatus.PASS, 105.0, 100.0, "psi", null);

		JsonNode json = mapper.valueToTree(ComplianceCheckDto.from(check));

		assertFalse(json.has("basis"));
		assertEquals("PASS", json.get("status").asString());
		assertEquals("10.2.6.1", json.get("clause").asString());
		assertEquals(105.0, json.get("actual").asDouble());
		assertTrue(json.get("note").isNull());
	}

	@Test
	void aCheckWithABasis_sendsItByName() {
		ComplianceCheck check = new ComplianceCheck("A.8.2.6", "Heat release", CheckBasis.SUPPLEMENTARY, CheckStatus.CONFIRM, null, null, null, null);

		JsonNode json = mapper.valueToTree(ComplianceCheckDto.from(check));

		assertEquals("SUPPLEMENTARY", json.get("basis").asString());
		assertEquals("CONFIRM", json.get("status").asString());
	}

}
