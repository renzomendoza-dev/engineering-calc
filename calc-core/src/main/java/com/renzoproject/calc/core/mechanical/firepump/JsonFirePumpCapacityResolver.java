package com.renzoproject.calc.core.mechanical.firepump;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

/**
 * Loads {@code reference/firepump/standard-capacities.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach: fresh {@code ObjectMapper}, hardcoded
 * resource path constant, {@code CalculationException} on a missing/unreadable resource.
 */
public class JsonFirePumpCapacityResolver implements FirePumpCapacityResolver {

	private static final String RESOURCE_PATH = "/reference/firepump/standard-capacities.json";

	private final List<Integer> capacities;

	public JsonFirePumpCapacityResolver() {
		this.capacities = loadCapacities();
	}

	private static List<Integer> loadCapacities() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonFirePumpCapacityResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, StandardCapacitiesFile.class).capacities();
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public StandardPumpRating resolveNextStandardCapacity(Quantity<VolumetricFlowRate> ratedFlow) {
		if (ratedFlow == null) {
			throw new CalculationException("ratedFlow is required");
		}
		double ratedFlowGpm = ratedFlow.to(FirePumpUnits.GPM).getValue().doubleValue();

		int chosen = capacities.stream()
				.filter(capacity -> capacity >= ratedFlowGpm)
				.min(Comparator.naturalOrder())
				.orElseThrow(() -> new CalculationException("No standard fire pump capacity >= " + ratedFlowGpm
						+ " GPM (largest available: " + capacities.stream().max(Comparator.naturalOrder()).orElse(0) + " GPM)"));

		return new StandardPumpRating(Quantities.getQuantity(chosen, FirePumpUnits.GPM));
	}

}
