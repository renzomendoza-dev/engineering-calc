package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.Comparator;
import java.util.List;

/**
 * Minimal in-memory {@link PumpMotorSizeResolver} for {@link PumpPowerCalculatorTest}, so it
 * isn't coupled to real reference data values.
 */
class FakePumpMotorSizeResolver implements PumpMotorSizeResolver {

	private final List<Double> steps;

	FakePumpMotorSizeResolver(List<Double> steps) {
		this.steps = steps;
	}

	@Override
	public double resolveNextStandardMotorKw(double shaftPowerKw) {
		return steps.stream()
				.filter(kw -> kw >= shaftPowerKw)
				.min(Comparator.naturalOrder())
				.orElseThrow(() -> new CalculationException("Fake resolver: no step >= " + shaftPowerKw));
	}

}
