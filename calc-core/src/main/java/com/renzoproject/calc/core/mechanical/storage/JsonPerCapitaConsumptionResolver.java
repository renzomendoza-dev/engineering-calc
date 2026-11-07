package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads {@code reference/storage/lpcd-consumption.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach: read through {@code ReferenceJson}, hardcoded
 * resource path constant, {@code CalculationException} on a missing/unreadable resource, parsed
 * eagerly in the constructor and cached rather than re-read per call.
 */
public class JsonPerCapitaConsumptionResolver implements PerCapitaConsumptionResolver {

	private static final String RESOURCE_PATH = "/reference/storage/lpcd-consumption.json";

	private final List<OccupancyTypeRow> rows;
	private final Map<String, Double> lpcdByOccupancyType;

	public JsonPerCapitaConsumptionResolver() {
		LpcdConsumptionFile file = load();
		this.rows = file.occupancyTypes();
		this.lpcdByOccupancyType = file.occupancyTypes().stream()
				.collect(Collectors.toMap(OccupancyTypeRow::type, OccupancyTypeRow::lpcd));
	}

	private static LpcdConsumptionFile load() {
		return ReferenceJson.read(RESOURCE_PATH, LpcdConsumptionFile.class);
	}

	@Override
	public double resolveLpcd(String occupancyType) {
		if (occupancyType == null) {
			throw new CalculationException("occupancyType is required");
		}
		Double lpcd = lpcdByOccupancyType.get(occupancyType);
		if (lpcd == null) {
			throw new CalculationException("Unknown occupancy type: " + occupancyType
					+ " (known types: " + lpcdByOccupancyType.keySet() + ")");
		}
		return lpcd;
	}

	@Override
	public List<OccupancyTypeRow> allEntries() {
		return rows;
	}

}
