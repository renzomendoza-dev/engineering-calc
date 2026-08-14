package com.renzoproject.calc.core.electrical.motorflc;

import com.renzoproject.calc.core.electrical.reference.ConductorMaterial;
import com.renzoproject.calc.core.electrical.reference.ConduitMaterial;
import com.renzoproject.calc.core.electrical.reference.InsulationType;
import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;
import com.renzoproject.calc.core.electrical.wiresizing.VoltageDropCheckRequest;
import com.renzoproject.calc.core.exception.CalculationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MotorConductorSizingCalculatorTest {

	private static final double DELTA = 1e-9;

	private final MotorConductorSizingCalculator calculator = new MotorConductorSizingCalculator();

	@Test
	void straightforwardCase_requiredAmpacityIsExactlyFlcTimes1point25() {
		// THREE_PHASE INDUCTION "10" HP @ 230V = 28A published FLC.
		MotorConductorSizingInput input = new MotorConductorSizingInput(
				MotorPhaseType.THREE_PHASE, MotorClass.INDUCTION, "10", 230, null,
				30.0, 2, InsulationType.THHN, ConductorMaterial.COPPER, 75, null);

		MotorConductorSizingResult result = calculator.calculate(input);

		assertEquals(28.0, result.motorFlcResult().flcAmps(), DELTA);
		// The critical assertion: requiredAmpacityAmps must be exactly flcAmps * 1.25, proving
		// the 125% factor was applied exactly once (via isContinuousLoad), not zero or two times.
		assertEquals(result.motorFlcResult().flcAmps() * 1.25, result.wireSizingResult().requiredAmpacityAmps(), DELTA);
		assertEquals(35.0, result.wireSizingResult().requiredAmpacityAmps(), DELTA);
	}

	@Test
	void synchronousMotorWithPowerFactor_adjustedFlcFlowsThroughAsLoadCurrent() {
		// THREE_PHASE SYNCHRONOUS "25" HP @ 230V = 53A base (unity PF), 90% PF multiplier = 1.1
		// -> adjusted flcAmps = 58.3A. requiredAmpacityAmps must be based on the ADJUSTED value,
		// not baseFlcAmps.
		MotorConductorSizingInput input = new MotorConductorSizingInput(
				MotorPhaseType.THREE_PHASE, MotorClass.SYNCHRONOUS, "25", 230, 90,
				30.0, 2, InsulationType.THHN, ConductorMaterial.COPPER, 75, null);

		MotorConductorSizingResult result = calculator.calculate(input);

		assertEquals(53.0, result.motorFlcResult().baseFlcAmps(), DELTA);
		assertEquals(58.3, result.motorFlcResult().flcAmps(), DELTA);
		assertEquals(58.3 * 1.25, result.wireSizingResult().requiredAmpacityAmps(), DELTA);
	}

	@Test
	void voltageDropCheckProvided_wireSizingResultIncludesIt() {
		VoltageDropCheckRequest voltageDropCheck = new VoltageDropCheckRequest(
				CircuitType.THREE_PHASE_AC, 10.0, 0.9, 230.0, ConduitMaterial.PVC, 1);
		MotorConductorSizingInput input = new MotorConductorSizingInput(
				MotorPhaseType.THREE_PHASE, MotorClass.INDUCTION, "10", 230, null,
				30.0, 2, InsulationType.THHN, ConductorMaterial.COPPER, 75, voltageDropCheck);

		MotorConductorSizingResult result = calculator.calculate(input);

		assertNotNull(result.wireSizingResult().voltageDropCheckResult());
		assertEquals(result.wireSizingResult().recommendedSizeLabel(),
				result.wireSizingResult().voltageDropCheckResult().sizeLabelChecked());
	}

	@Test
	void voltageDropCheckNotProvided_wireSizingResultOmitsIt() {
		MotorConductorSizingInput input = new MotorConductorSizingInput(
				MotorPhaseType.THREE_PHASE, MotorClass.INDUCTION, "10", 230, null,
				30.0, 2, InsulationType.THHN, ConductorMaterial.COPPER, 75, null);

		MotorConductorSizingResult result = calculator.calculate(input);

		assertNull(result.wireSizingResult().voltageDropCheckResult());
	}

	@Test
	void invalidMotorInput_threePhaseWithNullMotorClass_throwsCalculationException() {
		assertThrows(CalculationException.class, () -> new MotorConductorSizingInput(
				MotorPhaseType.THREE_PHASE, null, "10", 230, null,
				30.0, 2, InsulationType.THHN, ConductorMaterial.COPPER, 75, null));
	}

}
