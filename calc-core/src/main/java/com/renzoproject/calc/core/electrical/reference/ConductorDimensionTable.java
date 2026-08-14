package com.renzoproject.calc.core.electrical.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads PEC Table 10.1.1.5 (insulated conductor dimensions) from the classpath once, then
 * serves per insulation-type, per-size lookups of approximate cross-sectional area.
 */
public class ConductorDimensionTable {

	private static final String RESOURCE_PATH = "/reference/electrical/table-10-1-1-5.json";

	private final Map<String, ConductorDimensionEntry> entries;

	public ConductorDimensionTable() {
		this.entries = loadEntries();
	}

	private static Map<String, ConductorDimensionEntry> loadEntries() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = ConductorDimensionTable.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			ConductorDimensionEntry[] rows = objectMapper.readValue(in, ConductorDimensionEntry[].class);
			Map<String, ConductorDimensionEntry> byKey = new HashMap<>();
			for (ConductorDimensionEntry row : rows) {
				byKey.put(key(row.insulationType(), row.sizeLabel(), row.withoutOuterCovering()), row);
			}
			return byKey;
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	private static String key(InsulationType type, String sizeLabel, boolean withoutOuterCovering) {
		return type.name() + "|" + sizeLabel + "|" + withoutOuterCovering;
	}

	/**
	 * Defaults to the {@code withoutOuterCovering = false} variant. Use the overload for
	 * explicit control when both variants exist for a type/size.
	 *
	 * @throws CalculationException if the combination isn't found
	 */
	public double lookup(InsulationType type, String sizeLabel) {
		return lookup(type, sizeLabel, false);
	}

	/**
	 * @throws CalculationException if the combination isn't found
	 */
	public double lookup(InsulationType type, String sizeLabel, boolean withoutOuterCovering) {
		ConductorDimensionEntry entry = entries.get(key(type, sizeLabel, withoutOuterCovering));
		if (entry == null) {
			throw new CalculationException("No dimension data for insulation type " + type.toLabel()
					+ ", size " + sizeLabel + ", withoutOuterCovering=" + withoutOuterCovering);
		}
		return entry.approximateAreaMm2();
	}

	/**
	 * Distinct size labels with published dimension data for the given insulation type,
	 * ascending numerically. Intended for populating a caller's "sizes available for this
	 * insulation type" listing (e.g. a frontend dropdown for conduit fill).
	 */
	public List<String> sizeLabelsFor(InsulationType type) {
		return entries.values().stream()
				.filter(entry -> entry.insulationType() == type)
				.map(ConductorDimensionEntry::sizeLabel)
				.distinct()
				.sorted(Comparator.comparingDouble(Double::parseDouble))
				.toList();
	}

	/**
	 * All raw table rows, as published, ascending by insulation type label then size.
	 * Intended for displaying the table itself (e.g. a frontend reference table), as opposed
	 * to {@link #lookup} which resolves a single insulation-type/size combination.
	 */
	public List<ConductorDimensionEntry> allEntries() {
		return entries.values().stream()
				.sorted(Comparator
						.comparing((ConductorDimensionEntry entry) -> entry.insulationType().toLabel())
						.thenComparingDouble(entry -> Double.parseDouble(entry.sizeLabel()))
						.thenComparing(ConductorDimensionEntry::withoutOuterCovering))
				.toList();
	}

}
