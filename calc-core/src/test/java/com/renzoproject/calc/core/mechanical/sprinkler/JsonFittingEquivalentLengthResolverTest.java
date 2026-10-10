package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFittingEquivalentLengthResolverTest {

	private final JsonFittingEquivalentLengthResolver resolver = new JsonFittingEquivalentLengthResolver();

	@Test
	void publishedLengthsAreBasedOnC120() {
		assertEquals(120.0, resolver.cFactorBasis(), 1e-9);
	}

	@Test
	void everyFittingTypeIsPresentButUnpopulated() {
		// The schema ships with no values on purpose -- see reference/sprinkler/README.md. This
		// test is the tripwire for that decision: once the published table is entered it fails,
		// at which point the data is no longer a placeholder and this expectation should flip.
		assertEquals(7, resolver.allEntries().size());
		assertTrue(resolver.allEntries().stream()
						.allMatch(row -> row.equivalentLengthFtByNominalSize().isEmpty()),
				"expected no equivalent lengths to be populated yet");
		assertTrue(resolver.allEntries().stream().allMatch(row -> "placeholder".equals(row.confidence())),
				"expected every fitting row to be tagged 'placeholder'");
	}

	@Test
	void resolve_knownFittingType_returnsItsRow() {
		FittingEquivalentLengthRow row = resolver.resolve("ELBOW_90_STANDARD");

		assertEquals("ELBOW_90_STANDARD", row.fittingType());
	}

	@Test
	void resolve_isCaseInsensitive() {
		assertEquals("GATE_VALVE", resolver.resolve("gate_valve").fittingType());
	}

	@Test
	void resolve_unknownFittingType_throws() {
		assertThrows(CalculationException.class, () -> resolver.resolve("FLUX_CAPACITOR"));
	}

}
