package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.List;

/**
 * Loads {@code reference/sprinkler/hazen-williams-c.json} from the classpath once, same loading
 * approach as {@link JsonHazardPresetResolver}.
 */
public class JsonCFactorResolver implements CFactorResolver {

	private static final String RESOURCE_PATH = "/reference/sprinkler/hazen-williams-c.json";

	private final List<CFactorRow> rows;

	public JsonCFactorResolver() {
		this.rows = load().cFactors();
	}

	private static CFactorFile load() {
		return ReferenceJson.read(RESOURCE_PATH, CFactorFile.class);
	}

	@Override
	public CFactorRow resolve(String pipeType, SprinklerSystemType systemType) {
		if (pipeType == null || pipeType.isBlank()) {
			throw new CalculationException("pipeType is required");
		}
		if (systemType == null) {
			throw new CalculationException("systemType is required");
		}

		return rows.stream()
				.filter(row -> row.pipeType().equalsIgnoreCase(pipeType))
				.filter(row -> row.systemType().equalsIgnoreCase(systemType.name())
						|| row.systemType().equalsIgnoreCase(CFactorRow.ANY_SYSTEM_TYPE))
				// An exact systemType match sorts before the ANY fallback.
				.min((left, right) -> Boolean.compare(
						left.systemType().equalsIgnoreCase(CFactorRow.ANY_SYSTEM_TYPE),
						right.systemType().equalsIgnoreCase(CFactorRow.ANY_SYSTEM_TYPE)))
				.orElseThrow(() -> new CalculationException("No published Hazen-Williams C factor for pipeType="
						+ pipeType + ", systemType=" + systemType
						+ " (known pipe types: " + rows.stream().map(CFactorRow::pipeType).distinct().toList() + ")"));
	}

	@Override
	public List<CFactorRow> allEntries() {
		return rows;
	}

}
