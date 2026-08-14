package com.renzoproject.calc.core.electrical.motorflc;

import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MotorFlcCalculatorTest {

	private static final double DELTA = 1e-9;

	private final MotorFlcCalculator calculator = new MotorFlcCalculator();

	@Test
	void dcMotor_matchesHandVerifiedValues() {
		// DC "1" HP @ 230V = 4.7A (published). minimumConductorAmpacity = 4.7 * 1.25 = 5.875.
		MotorFlcInput input = new MotorFlcInput(MotorPhaseType.DC, null, "1", 230, null);

		MotorFlcResult result = calculator.calculate(input);

		assertEquals(4.7, result.flcAmps(), DELTA);
		assertEquals(4.7, result.baseFlcAmps(), DELTA);
		assertEquals(5.875, result.minimumConductorAmpacity(), DELTA);
	}

	@Test
	void singlePhaseInductionMotor_matchesHandVerifiedValues() {
		// SINGLE_PHASE "5" HP @ 230V = 28A (published). 28 * 1.25 = 35.0.
		MotorFlcInput input = new MotorFlcInput(MotorPhaseType.SINGLE_PHASE, null, "5", 230, null);

		MotorFlcResult result = calculator.calculate(input);

		assertEquals(28.0, result.flcAmps(), DELTA);
		assertEquals(28.0, result.baseFlcAmps(), DELTA);
		assertEquals(35.0, result.minimumConductorAmpacity(), DELTA);
	}

	@Test
	void threePhaseInductionMotor_matchesHandVerifiedValues() {
		// THREE_PHASE INDUCTION "10" HP @ 230V = 28A (published). 28 * 1.25 = 35.0.
		MotorFlcInput input = new MotorFlcInput(MotorPhaseType.THREE_PHASE, MotorClass.INDUCTION, "10", 230, null);

		MotorFlcResult result = calculator.calculate(input);

		assertEquals(28.0, result.flcAmps(), DELTA);
		assertEquals(28.0, result.baseFlcAmps(), DELTA);
		assertEquals(35.0, result.minimumConductorAmpacity(), DELTA);
	}

	@Test
	void threePhaseSynchronousMotor_withoutPowerFactor_flcEqualsBase() {
		// THREE_PHASE SYNCHRONOUS "25" HP @ 230V = 53A (published, at unity PF).
		MotorFlcInput input = new MotorFlcInput(MotorPhaseType.THREE_PHASE, MotorClass.SYNCHRONOUS, "25", 230, null);

		MotorFlcResult result = calculator.calculate(input);

		assertEquals(53.0, result.baseFlcAmps(), DELTA);
		assertEquals(53.0, result.flcAmps(), DELTA);
		assertEquals(66.25, result.minimumConductorAmpacity(), DELTA);
	}

	@Test
	void threePhaseSynchronousMotor_with90PercentPowerFactor_appliesMultiplier() {
		// Same base (53A), 90% PF multiplier = 1.1 -> flcAmps = 53 * 1.1 = 58.3.
		MotorFlcInput input = new MotorFlcInput(MotorPhaseType.THREE_PHASE, MotorClass.SYNCHRONOUS, "25", 230, 90);

		MotorFlcResult result = calculator.calculate(input);

		assertEquals(53.0, result.baseFlcAmps(), DELTA);
		assertEquals(58.3, result.flcAmps(), DELTA);
		assertEquals(58.3 * 1.25, result.minimumConductorAmpacity(), DELTA);
	}

	@Test
	void motorClassProvidedForNonThreePhase_throwsCalculationException() {
		assertThrows(CalculationException.class,
				() -> new MotorFlcInput(MotorPhaseType.SINGLE_PHASE, MotorClass.INDUCTION, "5", 230, null));
	}

	@Test
	void threePhaseWithNullMotorClass_throwsCalculationException() {
		assertThrows(CalculationException.class,
				() -> new MotorFlcInput(MotorPhaseType.THREE_PHASE, null, "10", 230, null));
	}

	@Test
	void powerFactorProvidedForInductionMotor_throwsCalculationException() {
		assertThrows(CalculationException.class,
				() -> new MotorFlcInput(MotorPhaseType.THREE_PHASE, MotorClass.INDUCTION, "10", 230, 90));
	}

}
