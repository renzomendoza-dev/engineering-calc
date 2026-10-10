package com.renzoproject.calc.core.mechanical.sprinkler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loads {@code reference/sprinkler/fitting-equivalent-lengths.json} from the classpath once, same
 * loading approach as {@link JsonHazardPresetResolver}.
 */
public class JsonFittingEquivalentLengthResolver implements FittingEquivalentLengthResolver {

	private static final String RESOURCE_PATH = "/reference/sprinkler/fitting-equivalent-lengths.json";

	private final List<FittingEquivalentLengthRow> rows;
	private final Map<String, FittingEquivalentLengthRow> rowsByType;
	private final double cFactorBasis;

	public JsonFittingEquivalentLengthResolver() {
		FittingEquivalentLengthFile file = load();
		this.rows = file.fittings();
		this.cFactorBasis = file.cFactorBasis();
		this.rowsByType = file.fittings().stream()
				.collect(Collectors.toMap(row -> row.fittingType().toUpperCase(), Function.identity()));
	}

	private static FittingEquivalentLengthFile load() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonFittingEquivalentLengthResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, FittingEquivalentLengthFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public FittingEquivalentLengthRow resolve(String fittingType) {
		if (fittingType == null || fittingType.isBlank()) {
			throw new CalculationException("fittingType is required");
		}
		FittingEquivalentLengthRow row = rowsByType.get(fittingType.toUpperCase());
		if (row == null) {
			throw new CalculationException("Unknown fitting type: " + fittingType
					+ " (known fitting types: " + rowsByType.keySet() + ")");
		}
		return row;
	}

	@Override
	public double cFactorBasis() {
		return cFactorBasis;
	}

	@Override
	public List<FittingEquivalentLengthRow> allEntries() {
		return rows;
	}

}
