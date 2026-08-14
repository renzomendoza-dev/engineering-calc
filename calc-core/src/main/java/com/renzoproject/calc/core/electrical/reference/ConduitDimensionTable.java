package com.renzoproject.calc.core.electrical.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads PEC Table 10.1.1.4 (conduit/tubing dimensions and percent-fill areas) from the
 * classpath once, then serves per-conduit-type lookups.
 */
public class ConduitDimensionTable {

	private static final String RESOURCE_PATH = "/reference/table-10-1-1-4.json";

	private final Map<ConduitType, List<ConduitDimensionEntry>> entriesByType;

	public ConduitDimensionTable() {
		this.entriesByType = loadEntries();
	}

	private static Map<ConduitType, List<ConduitDimensionEntry>> loadEntries() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = ConduitDimensionTable.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			ConduitDimensionEntry[] rows = objectMapper.readValue(in, ConduitDimensionEntry[].class);
			Map<ConduitType, List<ConduitDimensionEntry>> grouped = new HashMap<>();
			for (ConduitDimensionEntry row : rows) {
				grouped.computeIfAbsent(row.conduitType(), key -> new ArrayList<>()).add(row);
			}
			Map<ConduitType, List<ConduitDimensionEntry>> sorted = new HashMap<>();
			for (Map.Entry<ConduitType, List<ConduitDimensionEntry>> entry : grouped.entrySet()) {
				List<ConduitDimensionEntry> list = new ArrayList<>(entry.getValue());
				list.sort(Comparator.comparingInt(ConduitDimensionEntry::racewaySizeMm));
				sorted.put(entry.getKey(), List.copyOf(list));
			}
			return sorted;
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	/**
	 * @return entries for the given conduit type, ascending by {@code racewaySizeMm}
	 * @throws CalculationException if the type has no entries
	 */
	public List<ConduitDimensionEntry> getEntriesForType(ConduitType type) {
		List<ConduitDimensionEntry> entries = entriesByType.get(type);
		if (entries == null || entries.isEmpty()) {
			throw new CalculationException("No conduit dimension data for type " + type.toLabel());
		}
		return entries;
	}

	/**
	 * @throws CalculationException if the type has no entries, or no entry matches the size
	 */
	public ConduitDimensionEntry lookup(ConduitType type, int racewaySizeMm) {
		return getEntriesForType(type).stream()
				.filter(entry -> entry.racewaySizeMm() == racewaySizeMm)
				.findFirst()
				.orElseThrow(() -> new CalculationException(
						"No conduit dimension data for type " + type.toLabel() + ", raceway size " + racewaySizeMm));
	}

	/**
	 * All raw table rows, as published, ascending by conduit type label then raceway size.
	 * Intended for displaying the table itself (e.g. a frontend reference table), as opposed
	 * to {@link #lookup} which resolves a single conduit-type/size combination.
	 */
	public List<ConduitDimensionEntry> allEntries() {
		return entriesByType.values().stream()
				.flatMap(List::stream)
				.sorted(Comparator
						.comparing((ConduitDimensionEntry entry) -> entry.conduitType().toLabel())
						.thenComparingInt(ConduitDimensionEntry::racewaySizeMm))
				.toList();
	}

}
