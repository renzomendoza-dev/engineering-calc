package com.renzoproject.calc.core.mechanical.pump;

import com.renzoproject.calc.core.exception.CalculationException;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.Comparator;
import java.util.List;

/**
 * Loads {@code reference/pump/motor-kw-steps.json} from the classpath once. Mirrors
 * {@code JsonFirePumpMotorSizeResolver}'s loading approach (this is the SI/kW equivalent of
 * that HP/psi-GPM-package resolver — separate step table, not a unit conversion of it).
 */
public class JsonPumpMotorSizeResolver implements PumpMotorSizeResolver {

	private static final String RESOURCE_PATH = "/reference/pump/motor-kw-steps.json";

	private final List<Double> steps;

	public JsonPumpMotorSizeResolver() {
		this.steps = loadSteps();
	}

	private static List<Double> loadSteps() {
		return ReferenceJson.read(RESOURCE_PATH, MotorKwStepsFile.class).steps();
	}

	@Override
	public double resolveNextStandardMotorKw(double shaftPowerKw) {
		if (shaftPowerKw <= 0) {
			throw new CalculationException("shaftPowerKw must be positive");
		}
		return steps.stream()
				.filter(kw -> kw >= shaftPowerKw)
				.min(Comparator.naturalOrder())
				.orElseThrow(() -> new CalculationException("No standard motor kW step >= " + shaftPowerKw
						+ " kW (largest available: " + steps.stream().max(Comparator.naturalOrder()).orElse(0.0) + " kW)"));
	}

}
