package com.renzoproject.calc.core.electrical.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

/**
 * Loads PEC Table 3.10.2.6(B)(3)(a) (ampacity adjustment for more than three current-carrying
 * conductors) from the classpath once, then serves per-count lookups.
 */
public class ConductorCountAdjustmentTable {

	private static final String RESOURCE_PATH = "/reference/electrical/conductor-count-adjustment.json";

	private final List<ConductorCountAdjustmentEntry> rows;

	public ConductorCountAdjustmentTable() {
		this.rows = loadEntries();
	}

	private static List<ConductorCountAdjustmentEntry> loadEntries() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = ConductorCountAdjustmentTable.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return List.of(objectMapper.readValue(in, ConductorCountAdjustmentEntry[].class));
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	/**
	 * @return {@code 1.0} (no reduction) for 1-3 current-carrying conductors, since PEC Table
	 *         3.10.2.6(B)(3)(a) only applies beyond that; otherwise the matching row's
	 *         published percentage already divided by 100 (e.g. {@code 0.8} for a table value
	 *         of {@code 80})
	 * @throws CalculationException if conductorCount is less than 1
	 */
	public double lookup(int conductorCount) {
		if (conductorCount < 1) {
			throw new CalculationException("conductorCount must be at least 1");
		}
		if (conductorCount <= 3) {
			return 1.0;
		}
		for (ConductorCountAdjustmentEntry row : rows) {
			boolean withinMax = row.conductorCountMax() == null || conductorCount <= row.conductorCountMax();
			if (conductorCount >= row.conductorCountMin() && withinMax) {
				return row.adjustmentFactorPercent() / 100.0;
			}
		}
		throw new CalculationException("No conductor count adjustment factor found for count " + conductorCount);
	}

	/**
	 * All raw rows, as published, ascending by conductor count. Intended for displaying the
	 * table itself (e.g. a frontend reference table), as opposed to {@link #lookup} which
	 * resolves a single count.
	 */
	public List<ConductorCountAdjustmentEntry> allEntries() {
		return rows.stream()
				.sorted(Comparator.comparingInt(ConductorCountAdjustmentEntry::conductorCountMin))
				.toList();
	}

}
