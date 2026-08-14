package com.renzoproject.calc.core.electrical.motorflc;

import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LockedRotorCalculatorTest {

	private static final double DELTA = 1e-9;

	private final LockedRotorCalculator calculator = new LockedRotorCalculator();

	@Test
	void singlePhaseCase_matchesHandCheckedValue() {
		// Single-phase "5" HP @ 230V = 168A locked-rotor (published).
		LockedRotorInput input = new LockedRotorInput(false, "5", 230);

		LockedRotorResult result = calculator.calculate(input);

		assertEquals(168.0, result.lockedRotorAmps(), DELTA);
	}

	@Test
	void polyphaseCase_matchesHandCheckedValue() {
		// Design B/C/D polyphase "5" HP @ 230V = 92A locked-rotor (published).
		LockedRotorInput input = new LockedRotorInput(true, "5", 230);

		LockedRotorResult result = calculator.calculate(input);

		assertEquals(92.0, result.lockedRotorAmps(), DELTA);
	}

	@Test
	void unknownCombination_throwsCalculationException() {
		LockedRotorInput input = new LockedRotorInput(false, "9999", 230);

		assertThrows(CalculationException.class, () -> calculator.calculate(input));
	}

}
