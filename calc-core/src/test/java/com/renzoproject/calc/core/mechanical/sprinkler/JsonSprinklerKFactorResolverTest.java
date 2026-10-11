package com.renzoproject.calc.core.mechanical.sprinkler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSprinklerKFactorResolverTest {

	private final JsonSprinklerKFactorResolver resolver = new JsonSprinklerKFactorResolver();

	@Test
	void allEntries_coversThePublishedTable() {
		// NFPA 13 2019 Table 7.2.2.1 lists 13 nominal K-factors, 1.4 through 28.0.
		assertEquals(13, resolver.allEntries().size());
		assertEquals(1.4, resolver.allEntries().get(0).nominalK(), 1e-9);
		assertEquals(28.0, resolver.allEntries().get(12).nominalK(), 1e-9);
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
	void publishedColumnsMatchTheTable() {
		SprinklerKFactorRow k56 = row(5.6);
		assertEquals(80.0, k56.nominalKLpmBar(), 1e-9);
		assertEquals(5.3, k56.kFactorRangeMinGpm(), 1e-9);
		assertEquals(5.8, k56.kFactorRangeMaxGpm(), 1e-9);
		assertEquals(100.0, k56.percentOfK56Discharge(), 1e-9);
		assertEquals("1/2 in. (15 mm) NPT", k56.threadType());

		SprinklerKFactorRow k80 = row(8.0);
		assertEquals(140.0, k80.percentOfK56Discharge(), 1e-9);
		assertEquals(7.4, k80.kFactorRangeMinGpm(), 1e-9);
	}

	@Test
	void everyNominalKSitsInsideItsOwnPublishedRange() {
		for (SprinklerKFactorRow row : resolver.allEntries()) {
			assertTrue(row.nominalK() >= row.kFactorRangeMinGpm() && row.nominalK() <= row.kFactorRangeMaxGpm(),
					"nominal K " + row.nominalK() + " outside its range "
							+ row.kFactorRangeMinGpm() + "-" + row.kFactorRangeMaxGpm());
		}
	}

	@Test
	void everyRowIsVerifiedAgainstThePrintedStandard() {
		assertTrue(resolver.allEntries().stream().allMatch(row -> "verified".equals(row.confidence())),
				"expected every K-factor row to be tagged 'verified'");
	}

	private SprinklerKFactorRow row(double nominalK) {
		return resolver.allEntries().stream()
				.filter(candidate -> candidate.nominalK() == nominalK)
				.findFirst()
				.orElseThrow(() -> new AssertionError("no row for K=" + nominalK));
	}

}
