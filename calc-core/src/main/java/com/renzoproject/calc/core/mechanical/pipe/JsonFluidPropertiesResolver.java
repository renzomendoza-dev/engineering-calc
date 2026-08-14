package com.renzoproject.calc.core.mechanical.pipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Temperature;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads fluid property reference data from {@code reference/fluids/{fluidKey-lowercase}.json}
 * on the classpath, one file per fluid — mirrors {@code JsonPipeDimensionResolver}'s loading
 * approach.
 *
 * <p>Unlike pipe materials (a fixed, hardcoded set of four), fluids are open-ended — new fluid
 * JSON files can be added later without a code change (see {@code fluids-README.md}) — so files
 * are loaded lazily per requested {@code fluidKey} rather than all eagerly at construction, and
 * cached afterward to avoid re-parsing on repeated calls for the same fluid.
 */
public class JsonFluidPropertiesResolver implements FluidPropertiesResolver {

	private final Map<String, FluidPropertiesFile> cache = new HashMap<>();

	@Override
	public FluidProperties resolve(String fluidKey, Quantity<Temperature> temperature) {
		if (fluidKey == null || fluidKey.isBlank()) {
			throw new CalculationException("fluidKey is required");
		}
		if (temperature == null) {
			throw new CalculationException("temperature is required");
		}
		FluidPropertiesFile file = cache.computeIfAbsent(fluidKey.toUpperCase(), this::loadFile);
		double temperatureC = temperature.to(Units.CELSIUS).getValue().doubleValue();
		return interpolate(file, temperatureC, fluidKey);
	}

	private FluidPropertiesFile loadFile(String fluidKeyUpper) {
		String resourcePath = "/reference/fluids/" + fluidKeyUpper.toLowerCase() + ".json";
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonFluidPropertiesResolver.class.getResourceAsStream(resourcePath)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + resourcePath);
			}
			return objectMapper.readValue(in, FluidPropertiesFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + resourcePath, e);
		}
	}

	private static FluidProperties interpolate(FluidPropertiesFile file, double temperatureC, String fluidKey) {
		List<FluidPropertyRow> sorted = file.properties().stream()
				.sorted(Comparator.comparingDouble(FluidPropertyRow::temperatureC))
				.toList();

		double minC = sorted.get(0).temperatureC();
		double maxC = sorted.get(sorted.size() - 1).temperatureC();
		if (temperatureC < minC || temperatureC > maxC) {
			throw new CalculationException("Requested temperature " + temperatureC + " degC for fluid " + fluidKey
					+ " is outside the reference table's range (" + minC + " to " + maxC + " degC) -- cannot extrapolate");
		}

		for (int i = 0; i < sorted.size() - 1; i++) {
			double t1 = sorted.get(i).temperatureC();
			double t2 = sorted.get(i + 1).temperatureC();
			if (temperatureC >= t1 && temperatureC <= t2) {
				double fraction = Math.abs(t2 - t1) < 1e-9 ? 0.0 : (temperatureC - t1) / (t2 - t1);
				double density = sorted.get(i).density() + fraction * (sorted.get(i + 1).density() - sorted.get(i).density());
				double viscosity = sorted.get(i).dynamicViscosity()
						+ fraction * (sorted.get(i + 1).dynamicViscosity() - sorted.get(i).dynamicViscosity());
				return new FluidProperties(density, viscosity);
			}
		}

		throw new CalculationException("Unable to bracket " + temperatureC + " degC within fluid properties table for " + fluidKey);
	}

}
