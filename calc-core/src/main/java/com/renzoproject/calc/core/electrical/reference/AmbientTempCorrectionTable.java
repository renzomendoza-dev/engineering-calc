package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.Comparator;
import java.util.List;

/**
 * Loads PEC Table 3.10.2.6(B)(2)(a) (ambient temperature correction factors, 30C base) from
 * the classpath once, then serves per-ambient-temperature, per-conductor-temp-rating lookups.
 */
public class AmbientTempCorrectionTable {

	private static final String RESOURCE_PATH = "/reference/electrical/ambient-temp-correction-30c.json";

	private final List<AmbientTempCorrectionEntry> rows;

	public AmbientTempCorrectionTable() {
		this.rows = loadEntries();
	}

	private static List<AmbientTempCorrectionEntry> loadEntries() {
		return List.of(ReferenceJson.read(RESOURCE_PATH, AmbientTempCorrectionEntry[].class));
	}

	/**
	 * @throws CalculationException if {@code ambientTempCelsius} falls outside every published
	 *                               range, or the requested {@code tempRatingCelsius} has no
	 *                               published factor at the matching range (e.g. a 60C
	 *                               conductor at 65C ambient — not a valid combination)
	 */
	public double lookup(double ambientTempCelsius, int tempRatingCelsius) {
		AmbientTempCorrectionEntry row = findRow(ambientTempCelsius);
		Double factor = factorFor(row, tempRatingCelsius);
		if (factor == null) {
			throw new CalculationException("No ambient temperature correction factor for " + tempRatingCelsius
					+ "C conductors at " + ambientTempCelsius + "C ambient (range \"" + row.ambientTempRangeLabel() + "\")");
		}
		return factor;
	}

	private AmbientTempCorrectionEntry findRow(double ambientTempCelsius) {
		for (AmbientTempCorrectionEntry row : rows) {
			double low = row.ambientTempLowC() == null ? Double.NEGATIVE_INFINITY : row.ambientTempLowC();
			if (ambientTempCelsius >= low && ambientTempCelsius <= row.ambientTempHighC()) {
				return row;
			}
		}
		throw new CalculationException(
				"Ambient temperature " + ambientTempCelsius + "C is outside the published correction table range");
	}

	private static Double factorFor(AmbientTempCorrectionEntry row, int tempRatingCelsius) {
		return switch (tempRatingCelsius) {
			case 60 -> row.factor60C();
			case 75 -> row.factor75C();
			case 90 -> row.factor90C();
			default -> throw new CalculationException("Unsupported conductor temp rating: " + tempRatingCelsius + "C");
		};
	}

	/**
	 * All raw rows, as published, ascending by ambient temperature range. Intended for
	 * displaying the table itself (e.g. a frontend reference table), as opposed to
	 * {@link #lookup} which resolves a single ambient temperature.
	 */
	public List<AmbientTempCorrectionEntry> allEntries() {
		return rows.stream()
				.sorted(Comparator.comparingDouble(AmbientTempCorrectionEntry::ambientTempHighC))
				.toList();
	}

}
