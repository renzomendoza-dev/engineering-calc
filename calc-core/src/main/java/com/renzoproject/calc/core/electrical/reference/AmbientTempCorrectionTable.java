package com.renzoproject.calc.core.electrical.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads PEC Table 3.10.2.6(B)(2)(a) (ambient temperature correction factors, 30C base) from
 * the classpath once, then serves per-ambient-temperature, per-conductor-temp-rating lookups.
 */
public class AmbientTempCorrectionTable {

	private static final String RESOURCE_PATH = "/reference/electrical/ambient-temp-correction-30c.json";

	private final List<Row> rows;

	public AmbientTempCorrectionTable() {
		this.rows = loadEntries();
	}

	private static List<Row> loadEntries() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = AmbientTempCorrectionTable.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return List.of(objectMapper.readValue(in, Row[].class));
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	/**
	 * @throws CalculationException if {@code ambientTempCelsius} falls outside every published
	 *                               range, or the requested {@code tempRatingCelsius} has no
	 *                               published factor at the matching range (e.g. a 60C
	 *                               conductor at 65C ambient — not a valid combination)
	 */
	public double lookup(double ambientTempCelsius, int tempRatingCelsius) {
		Row row = findRow(ambientTempCelsius);
		Double factor = factorFor(row, tempRatingCelsius);
		if (factor == null) {
			throw new CalculationException("No ambient temperature correction factor for " + tempRatingCelsius
					+ "C conductors at " + ambientTempCelsius + "C ambient (range \"" + row.ambientTempRangeLabel + "\")");
		}
		return factor;
	}

	private Row findRow(double ambientTempCelsius) {
		for (Row row : rows) {
			double low = row.ambientTempLowC == null ? Double.NEGATIVE_INFINITY : row.ambientTempLowC;
			if (ambientTempCelsius >= low && ambientTempCelsius <= row.ambientTempHighC) {
				return row;
			}
		}
		throw new CalculationException(
				"Ambient temperature " + ambientTempCelsius + "C is outside the published correction table range");
	}

	private static Double factorFor(Row row, int tempRatingCelsius) {
		return switch (tempRatingCelsius) {
			case 60 -> row.factor60C;
			case 75 -> row.factor75C;
			case 90 -> row.factor90C;
			default -> throw new CalculationException("Unsupported conductor temp rating: " + tempRatingCelsius + "C");
		};
	}

	private record Row(
			String ambientTempRangeLabel,
			Double ambientTempLowC,
			double ambientTempHighC,
			Double factor60C,
			Double factor75C,
			Double factor90C) {
	}

}
