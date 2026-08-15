package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossCalculator;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossInput;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossResult;

/**
 * Stub {@link PipePressureLossCalculator} that always throws, for
 * {@link PumpTDHCalculatorTest}'s nested-exception-propagation case.
 */
class ThrowingStubPipePressureLossCalculator extends PipePressureLossCalculator {

	private final String message;

	ThrowingStubPipePressureLossCalculator(String message) {
		super(null, null, null);
		this.message = message;
	}

	@Override
	public PipePressureLossResult calculate(PipePressureLossInput input) {
		throw new CalculationException(message);
	}

}
