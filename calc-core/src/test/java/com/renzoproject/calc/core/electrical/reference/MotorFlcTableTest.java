package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotorFlcTableTest {

	private static final double DELTA = 1e-9;

	private final MotorFlcTable table = new MotorFlcTable();

	@Test
	void lookup_threePhaseInduction10Hp230V_matchesPublishedValue() {
		assertEquals(28.0, table.lookup(MotorPhaseType.THREE_PHASE, MotorClass.INDUCTION, "10", 230), DELTA);
	}

	@Test
	void lookup_singlePhase5Hp230V_matchesPublishedValue() {
		assertEquals(28.0, table.lookup(MotorPhaseType.SINGLE_PHASE, null, "5", 230), DELTA);
	}

	@Test
	void lookup_threePhaseSynchronous25Hp230V_matchesPublishedValue() {
		assertEquals(53.0, table.lookup(MotorPhaseType.THREE_PHASE, MotorClass.SYNCHRONOUS, "25", 230), DELTA);
	}

	@Test
	void lookup_nonThreePhase_ignoresMotorClassEvenWhenProvided() {
		// motorClass is meaningless for DC; passing a non-null value must not change the result
		// or fail — it's normalized away rather than causing a mismatch.
		double withNull = table.lookup(MotorPhaseType.DC, null, "1", 230);
		double withIgnoredClass = table.lookup(MotorPhaseType.DC, MotorClass.SYNCHRONOUS, "1", 230);

		assertEquals(4.7, withNull, DELTA);
		assertEquals(withNull, withIgnoredClass, DELTA);
	}

	@Test
	void lookup_unpublishedCombination_throwsCalculationException() {
		assertThrows(CalculationException.class,
				() -> table.lookup(MotorPhaseType.THREE_PHASE, MotorClass.INDUCTION, "5000", 230));
	}

	@Test
	void sizeLabelsFor_returnsAscendingDistinctLabelsForPhaseAndClass() {
		List<String> labels = table.sizeLabelsFor(MotorPhaseType.THREE_PHASE, MotorClass.INDUCTION);

		assertTrue(labels.contains("10"));
		assertEquals(labels.size(), labels.stream().distinct().count());
		for (int i = 1; i < labels.size(); i++) {
			assertTrue(HorsepowerRating.parseHp(labels.get(i - 1)) < HorsepowerRating.parseHp(labels.get(i)));
		}
	}

	@Test
	void sizeLabelsFor_synchronousOnlyIncludes25HpAndAbove() {
		// Per the source table's footnote: synchronous entries only exist at 25 HP and above.
		List<String> labels = table.sizeLabelsFor(MotorPhaseType.THREE_PHASE, MotorClass.SYNCHRONOUS);

		assertTrue(labels.contains("25"));
		assertTrue(labels.stream().allMatch(label -> HorsepowerRating.parseHp(label) >= 25.0));
	}

	@Test
	void voltagesFor_returnsAscendingDistinctVoltages() {
		List<Integer> voltages = table.voltagesFor(MotorPhaseType.THREE_PHASE, MotorClass.INDUCTION, "10");

		assertTrue(voltages.contains(230));
		assertEquals(voltages.size(), voltages.stream().distinct().count());
		for (int i = 1; i < voltages.size(); i++) {
			assertTrue(voltages.get(i - 1) < voltages.get(i));
		}
	}

}
