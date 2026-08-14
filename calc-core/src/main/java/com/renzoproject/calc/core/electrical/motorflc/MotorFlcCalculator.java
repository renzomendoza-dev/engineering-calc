package com.renzoproject.calc.core.electrical.motorflc;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorFlcTable;
import com.renzoproject.calc.core.electrical.reference.SynchronousPowerFactorAdjustmentTable;

/**
 * Looks up a motor's published full-load current and derives the minimum conductor ampacity
 * for continuous-duty single-motor branch circuits per PEC Article 4.30.
 *
 * <p>Needs reference data — like the other table-backed calculators in this codebase,
 * {@link MotorFlcTable} and {@link SynchronousPowerFactorAdjustmentTable} are plainly
 * instantiated in the constructor rather than injected as beans (calc-core has no Spring
 * dependency, so there's no DI to reach for here).
 */
public class MotorFlcCalculator implements Calculator<MotorFlcInput, MotorFlcResult> {

	private static final double CONTINUOUS_DUTY_FACTOR = 1.25;

	private final MotorFlcTable motorFlcTable = new MotorFlcTable();
	private final SynchronousPowerFactorAdjustmentTable powerFactorAdjustmentTable = new SynchronousPowerFactorAdjustmentTable();

	@Override
	public MotorFlcResult calculate(MotorFlcInput input) {
		double baseFlcAmps = motorFlcTable.lookup(input.phaseType(), input.motorClass(), input.horsepowerLabel(), input.voltage());

		double flcAmps = baseFlcAmps;
		if (input.motorClass() == MotorClass.SYNCHRONOUS && input.synchronousPowerFactorPercent() != null) {
			double multiplier = powerFactorAdjustmentTable.lookup(input.synchronousPowerFactorPercent());
			flcAmps = baseFlcAmps * multiplier;
		}

		double minimumConductorAmpacity = flcAmps * CONTINUOUS_DUTY_FACTOR;

		return new MotorFlcResult(flcAmps, baseFlcAmps, minimumConductorAmpacity);
	}

}
