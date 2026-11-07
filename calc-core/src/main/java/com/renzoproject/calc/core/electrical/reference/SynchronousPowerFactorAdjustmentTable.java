package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the synchronous motor power factor adjustment footnote from PEC Table 4.30.14.4
 * (published FLC values are at unity power factor; this table's multipliers correct for 90%
 * and 80% PF) from the classpath once, then serves per-percentage lookups.
 */
public class SynchronousPowerFactorAdjustmentTable {

	private static final String RESOURCE_PATH = "/reference/electrical/synchronous-motor-pf-adjustment.json";

	private final Map<Integer, Double> multipliers;

	public SynchronousPowerFactorAdjustmentTable() {
		this.multipliers = loadEntries();
	}

	private static Map<Integer, Double> loadEntries() {
		Row[] rows = ReferenceJson.read(RESOURCE_PATH, Row[].class);
		Map<Integer, Double> byPercent = new HashMap<>();
		for (Row row : rows) {
			byPercent.put(row.powerFactorPercent(), row.multiplier());
		}
		return byPercent;
	}

	/**
	 * @throws CalculationException if powerFactorPercent isn't exactly 100, 90, or 80 — the
	 *                               published footnote does not support interpolation
	 */
	public double lookup(int powerFactorPercent) {
		Double multiplier = multipliers.get(powerFactorPercent);
		if (multiplier == null) {
			throw new CalculationException(
					"Unsupported synchronous motor power factor: " + powerFactorPercent + "% (only 100, 90, or 80 are published)");
		}
		return multiplier;
	}

	private record Row(int powerFactorPercent, double multiplier) {
	}

}
