package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

import com.renzoproject.calc.core.reference.ReferenceJson;
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
		return ReferenceJson.read(RESOURCE_PATH, FittingEquivalentLengthFile.class);
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
