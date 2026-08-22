package com.renzoproject.calc.core.mechanical.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads {@code reference/storage/fire-water-duration.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach: fresh {@code ObjectMapper}, hardcoded
 * resource path constant, {@code CalculationException} on a missing/unreadable resource, parsed
 * eagerly in the constructor and cached rather than re-read per call. No interpolation needed --
 * flat lookup by classification, same as {@link JsonPerCapitaConsumptionResolver}.
 */
public class JsonFireWaterDurationResolver implements FireWaterDurationResolver {

	private static final String RESOURCE_PATH = "/reference/storage/fire-water-duration.json";

	private final Map<HazardClassification, DurationRange> rangesByClassification;
	private final List<FireWaterDurationEntry> entries;

	public JsonFireWaterDurationResolver() {
		FireWaterDurationFile file = load();
		this.rangesByClassification = file.classifications().stream()
				.collect(Collectors.toMap(
						row -> HazardClassification.valueOf(row.hazardClassification()),
						row -> new DurationRange(row.durationMinutesMin(), row.durationMinutesMax(), row.combinedHoseGpm())));
		this.entries = file.classifications().stream()
				.map(row -> new FireWaterDurationEntry(
						HazardClassification.valueOf(row.hazardClassification()),
						row.durationMinutesMin(),
						row.durationMinutesMax(),
						row.combinedHoseGpm()))
				.toList();
	}

	private static FireWaterDurationFile load() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonFireWaterDurationResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, FireWaterDurationFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public DurationRange resolve(HazardClassification classification) {
		if (classification == null) {
			throw new CalculationException("classification is required");
		}
		DurationRange range = rangesByClassification.get(classification);
		if (range == null) {
			throw new CalculationException("No duration data for hazard classification: " + classification);
		}
		return range;
	}

	@Override
	public List<FireWaterDurationEntry> allEntries() {
		return entries;
	}

}
