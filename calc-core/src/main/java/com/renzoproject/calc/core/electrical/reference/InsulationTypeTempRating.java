package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.exception.CalculationException;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the insulation-type-to-temperature-rating mapping from the classpath once, then
 * serves per insulation-type, per-material lookups of which {@link AmpacityTable} temperature
 * column applies.
 *
 * <p>PEC Table 3.10.3.1(A) rates THHW at 75 C in wet locations and 90 C in dry ones, and XHHW at
 * 90 C in dry and damp locations; the ampacity table (3.10.2.6(B)(16)) lists both under 75 C. A
 * lookup gives one rating, so THHW and XHHW are held to 75 C, the lower, which is right in a wet
 * location and conservative in a dry one. The 90 C rows are not in the file for that reason.
 */
public class InsulationTypeTempRating {

	private static final String RESOURCE_PATH = "/reference/electrical/insulation-type-temp-rating.json";

	private final List<InsulationTypeTempRatingEntry> rows;
	private final Map<String, Integer> byKey;

	public InsulationTypeTempRating() {
		this.rows = loadEntries();
		Map<String, Integer> byKey = new HashMap<>();
		for (InsulationTypeTempRatingEntry row : rows) {
			byKey.put(key(row.insulationType(), row.conductorMaterial()), row.tempRatingCelsius());
		}
		this.byKey = byKey;
	}

	private static List<InsulationTypeTempRatingEntry> loadEntries() {
		return List.of(ReferenceJson.read(RESOURCE_PATH, InsulationTypeTempRatingEntry[].class));
	}

	private static String key(InsulationType type, ConductorMaterial material) {
		return type.name() + "|" + material.name();
	}

	/**
	 * @throws CalculationException if the insulation type has no published temperature rating
	 *                               for that material (e.g. ZW is copper-only)
	 */
	public int lookup(InsulationType type, ConductorMaterial material) {
		Integer tempRating = byKey.get(key(type, material));
		if (tempRating == null) {
			throw new CalculationException("No published temperature rating for insulation type "
					+ type.toLabel() + " with conductor material " + material);
		}
		return tempRating;
	}

	/**
	 * All raw rows, as published, ascending by temperature rating then insulation type.
	 * Intended for displaying the table itself (e.g. a frontend reference table), as opposed
	 * to {@link #lookup} which resolves a single combination.
	 */
	public List<InsulationTypeTempRatingEntry> allEntries() {
		return rows.stream()
				.sorted(Comparator
						.comparingInt(InsulationTypeTempRatingEntry::tempRatingCelsius)
						.thenComparing(entry -> entry.insulationType().toLabel())
						.thenComparing(entry -> entry.conductorMaterial().name()))
				.toList();
	}

}
