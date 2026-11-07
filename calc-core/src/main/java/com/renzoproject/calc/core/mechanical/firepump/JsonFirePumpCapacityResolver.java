package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.exception.CalculationException;
import com.renzoproject.calc.core.mechanical.pipe.VolumetricFlowRate;
import com.renzoproject.calc.core.reference.ReferenceJson;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import java.util.Comparator;
import java.util.List;

/**
 * Loads {@code reference/firepump/standard-capacities.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach: read through {@code ReferenceJson}, hardcoded
 * resource path constant, {@code CalculationException} on a missing/unreadable resource.
 */
public class JsonFirePumpCapacityResolver implements FirePumpCapacityResolver {

	private static final String RESOURCE_PATH = "/reference/firepump/standard-capacities.json";

	private final List<Integer> capacities;

	public JsonFirePumpCapacityResolver() {
		this.capacities = loadCapacities();
	}

	private static List<Integer> loadCapacities() {
		return ReferenceJson.read(RESOURCE_PATH, StandardCapacitiesFile.class).capacities();
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
