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
	void everyPublishedFittingRowIsPopulated() {
		// NFPA 13 2019 Table 27.2.3.1.1 has 8 fitting rows; 102 of the 8x15 cells carry a value
		// and the rest are dashes, which are omitted rather than stored as zero.
		assertEquals(8, resolver.allEntries().size());
		assertTrue(resolver.allEntries().stream()
						.noneMatch(row -> row.equivalentLengthFtByNominalSize().isEmpty()),
				"every fitting type should have at least one published size");
		assertEquals(102, resolver.allEntries().stream()
				.mapToInt(row -> row.equivalentLengthFtByNominalSize().size())
				.sum());
	}

	@Test
	void transcribedValuesMatchThePublishedTable() {
		assertEquals(5.0, lengthFt("ELBOW_90_STANDARD", "2"), 1e-9);
		assertEquals(27.0, lengthFt("ELBOW_90_STANDARD", "12"), 1e-9);
		assertEquals(0.5, lengthFt("ELBOW_90_LONG_TURN", "1/2"), 1e-9);
		assertEquals(20.0, lengthFt("TEE_OR_CROSS_FLOW_TURNED_90", "4"), 1e-9);
		assertEquals(45.0, lengthFt("SWING_CHECK_VALVE", "8"), 1e-9);
		assertEquals(16.0, lengthFt("VANE_TYPE_FLOW_SWITCH", "6"), 1e-9);
	}

	@Test
	void butterflyValveIsNonMonotonicAsPublished() {
		// 12 ft at 4 in. but 9 ft at 5 in. -- an oddity of the printed table, transcribed rather
		// than smoothed over.
		assertEquals(12.0, lengthFt("BUTTERFLY_VALVE", "4"), 1e-9);
		assertEquals(9.0, lengthFt("BUTTERFLY_VALVE", "5"), 1e-9);
	}

	@Test
	void sizesTheTableLeavesBlankAreAbsent() {
		// A dash in the table means "not published", which must stay absent so the calculator
		// refuses the combination instead of treating it as zero length.
		assertEquals(null, resolver.resolve("BUTTERFLY_VALVE").equivalentLengthFtByNominalSize().get("1-1/2"));
		assertEquals(null, resolver.resolve("GATE_VALVE").equivalentLengthFtByNominalSize().get("1"));
		assertEquals(null, resolver.resolve("VANE_TYPE_FLOW_SWITCH").equivalentLengthFtByNominalSize().get("5"));
		assertEquals(null, resolver.resolve("ELBOW_45").equivalentLengthFtByNominalSize().get("1/2"));
	}

	@Test
	void everyRowHasPassedItsSecondRead_theVaneFlowSwitchIncluded() {
		// Every row was checked cell-for-cell against the printed 2019 Table 27.2.3.1.1. The vane
		// type flow switch was the last one outstanding: neither reproduction used as a second reader
		// carries it, but the printed table does, with these values.
		for (FittingEquivalentLengthRow row : resolver.allEntries()) {
			assertEquals("verified", row.confidence(), row.fittingType() + " should be tagged 'verified'");
		}
	}

	@Test
	void theTwoNonMonotonicButterflyCellsArePublished() {
		// 12 ft at 4 in. then 9 ft at 5 in. looks like a transcription slip and is not: both
		// independent reproductions carry the same pair. Pinned so nobody "fixes" it later.
		assertEquals(12.0, lengthFt("BUTTERFLY_VALVE", "4"), 1e-9);
		assertEquals(9.0, lengthFt("BUTTERFLY_VALVE", "5"), 1e-9);
		assertEquals(10.0, lengthFt("BUTTERFLY_VALVE", "6"), 1e-9);
	}

	private double lengthFt(String fittingType, String nominalSize) {
		Double value = resolver.resolve(fittingType).equivalentLengthFtByNominalSize().get(nominalSize);
		if (value == null) {
			throw new AssertionError("no published length for " + fittingType + " at " + nominalSize);
		}
		return value;
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
