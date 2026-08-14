package com.renzoproject.calc.core.mechanical.firepump;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

/**
 * Loads {@code reference/firepump/motor-hp-steps.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach.
 */
public class JsonFirePumpMotorSizeResolver implements FirePumpMotorSizeResolver {

	private static final String RESOURCE_PATH = "/reference/firepump/motor-hp-steps.json";

	private final List<Double> steps;

	public JsonFirePumpMotorSizeResolver() {
		this.steps = loadSteps();
	}

	private static List<Double> loadSteps() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonFirePumpMotorSizeResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, MotorHpStepsFile.class).steps();
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public double resolveNextStandardMotorHp(double brakeHorsepower) {
		if (brakeHorsepower <= 0) {
			throw new CalculationException("brakeHorsepower must be positive");
		}
		return steps.stream()
				.filter(hp -> hp >= brakeHorsepower)
				.min(Comparator.naturalOrder())
				.orElseThrow(() -> new CalculationException("No standard motor HP step >= " + brakeHorsepower
						+ " BHP (largest available: " + steps.stream().max(Comparator.naturalOrder()).orElse(0.0) + " HP)"));
	}

}
