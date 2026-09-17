package com.renzoproject.calc_api.common;

import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnumParsingTest {

	@Test
	void parse_exactConstantName_returnsConstant() {
		assertEquals(CircuitType.THREE_PHASE_AC, EnumParsing.parse(CircuitType.class, "THREE_PHASE_AC", "circuit type"));
	}

	@Test
	void parse_unknownValue_throwsWithFieldLabelAndValue() {
		CalculationException ex = assertThrows(CalculationException.class,
				() -> EnumParsing.parse(CircuitType.class, "FOUR_PHASE", "circuit type"));

		assertEquals("Unknown circuit type: FOUR_PHASE", ex.getMessage());
	}

	@Test
	void parse_isCaseSensitive() {
		// Matches Enum.valueOf semantics the four previous copies had; not silently widened here.
		assertThrows(CalculationException.class, () -> EnumParsing.parse(CircuitType.class, "dc", "circuit type"));
	}

	@Test
	void parse_null_throwsCalculationException_notNullPointerException() {
		// Enum.valueOf(type, null) throws NullPointerException, which would reach the client as a
		// 500 instead of a 400.
		CalculationException ex = assertThrows(CalculationException.class,
				() -> EnumParsing.parse(CircuitType.class, null, "circuit type"));

		assertEquals("circuit type is required", ex.getMessage());
	}

}
