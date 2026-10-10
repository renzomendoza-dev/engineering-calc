package com.renzoproject.calc.core.mechanical.sprinkler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSprinklerKFactorResolverTest {

	private final JsonSprinklerKFactorResolver resolver = new JsonSprinklerKFactorResolver();

	@Test
	void allEntries_includesTheTwoMostCommonKFactors() {
		assertTrue(resolver.allEntries().stream().anyMatch(row -> row.nominalK() == 5.6));
		assertTrue(resolver.allEntries().stream().anyMatch(row -> row.nominalK() == 8.0));
	}

	@Test
	void kFactorsAreAscending() {
		double previous = 0;
		for (SprinklerKFactorRow row : resolver.allEntries()) {
			assertTrue(row.nominalK() > previous,
					"expected ascending K factors, " + row.nominalK() + " followed " + previous);
			previous = row.nominalK();
		}
	}

	@Test
	void orificeSizeIsRecordedOnlyWhereKnown() {
		// Unconfirmed orifice sizes are null rather than guessed.
		SprinklerKFactorRow k56 = row(5.6);
		assertEquals("1/2 in.", k56.orificeNominal());
		assertEquals(null, row(22.4).orificeNominal());
	}

	private SprinklerKFactorRow row(double nominalK) {
		return resolver.allEntries().stream()
				.filter(candidate -> candidate.nominalK() == nominalK)
				.findFirst()
				.orElseThrow(() -> new AssertionError("no row for K=" + nominalK));
	}

}
