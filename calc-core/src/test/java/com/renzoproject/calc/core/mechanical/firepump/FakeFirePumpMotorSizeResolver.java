package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.Comparator;
import java.util.List;

class FakeFirePumpMotorSizeResolver implements FirePumpMotorSizeResolver {

	private final List<Double> steps;

	FakeFirePumpMotorSizeResolver(List<Double> steps) {
		this.steps = steps;
	}

	@Override
	public double resolveNextStandardMotorHp(double brakeHorsepower) {
		return steps.stream()
				.filter(hp -> hp >= brakeHorsepower)
				.min(Comparator.naturalOrder())
				.orElseThrow(() -> new CalculationException("Fake resolver: no step >= " + brakeHorsepower));
	}

}
