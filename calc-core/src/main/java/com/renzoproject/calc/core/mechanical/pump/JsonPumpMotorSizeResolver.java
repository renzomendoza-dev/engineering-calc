package com.renzoproject.calc.core.mechanical.pump;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
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
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonPumpMotorSizeResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, MotorKwStepsFile.class).steps();
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
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
