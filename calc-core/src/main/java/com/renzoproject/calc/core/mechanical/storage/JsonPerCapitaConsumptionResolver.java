package com.renzoproject.calc.core.mechanical.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads {@code reference/storage/lpcd-consumption.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach: fresh {@code ObjectMapper}, hardcoded
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
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonPerCapitaConsumptionResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, LpcdConsumptionFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
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
