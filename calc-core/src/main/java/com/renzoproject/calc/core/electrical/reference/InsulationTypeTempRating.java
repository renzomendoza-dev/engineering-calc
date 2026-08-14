package com.renzoproject.calc.core.electrical.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the insulation-type-to-temperature-rating mapping from the classpath once, then
 * serves per insulation-type, per-material lookups of which {@link AmpacityTable} temperature
 * column applies.
 */
public class InsulationTypeTempRating {

	private static final String RESOURCE_PATH = "/reference/insulation-type-temp-rating.json";

	private final Map<String, Integer> entries;

	public InsulationTypeTempRating() {
		this.entries = loadEntries();
	}

	private static Map<String, Integer> loadEntries() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = InsulationTypeTempRating.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			Row[] rows = objectMapper.readValue(in, Row[].class);
			Map<String, Integer> byKey = new HashMap<>();
			for (Row row : rows) {
				byKey.put(key(row.insulationType, row.conductorMaterial), row.tempRatingCelsius);
			}
			return byKey;
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	private static String key(InsulationType type, ConductorMaterial material) {
		return type.name() + "|" + material.name();
	}

	/**
	 * @throws CalculationException if the insulation type has no published temperature rating
	 *                               for that material (e.g. ZW is copper-only)
	 */
	public int lookup(InsulationType type, ConductorMaterial material) {
		Integer tempRating = entries.get(key(type, material));
		if (tempRating == null) {
			throw new CalculationException("No published temperature rating for insulation type "
					+ type.toLabel() + " with conductor material " + material);
		}
		return tempRating;
	}

	private record Row(InsulationType insulationType, ConductorMaterial conductorMaterial, int tempRatingCelsius) {
	}

}
