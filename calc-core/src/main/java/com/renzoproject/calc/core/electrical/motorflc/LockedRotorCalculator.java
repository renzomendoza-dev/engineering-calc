package com.renzoproject.calc.core.electrical.motorflc;

import com.renzoproject.calc.core.Calculator;
import com.renzoproject.calc.core.electrical.reference.LockedRotorPolyphaseTable;
import com.renzoproject.calc.core.electrical.reference.LockedRotorSinglePhaseTable;

/**
 * Looks up locked-rotor current for selecting disconnecting means and controllers (PEC
 * 4.30.9.10, 4.40.1.2, 4.40.5.1, 4.55.1.8(C)).
 *
 * <p>This is a distinct calculation purpose from FLC (used for conductor/branch-circuit
 * sizing, see {@link MotorFlcCalculator}) — deliberately a separate, smaller calculator
 * rather than combined with it.
 */
public class LockedRotorCalculator implements Calculator<LockedRotorInput, LockedRotorResult> {

	private final LockedRotorSinglePhaseTable singlePhaseTable = new LockedRotorSinglePhaseTable();
	private final LockedRotorPolyphaseTable polyphaseTable = new LockedRotorPolyphaseTable();

	@Override
	public LockedRotorResult calculate(LockedRotorInput input) {
		double lockedRotorAmps = input.isPolyphase()
				? polyphaseTable.lookup(input.horsepowerLabel(), input.voltage())
				: singlePhaseTable.lookup(input.horsepowerLabel(), input.voltage());
		return new LockedRotorResult(lockedRotorAmps);
	}

}
