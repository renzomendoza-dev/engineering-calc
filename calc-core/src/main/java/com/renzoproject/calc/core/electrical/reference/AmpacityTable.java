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
 * Loads PEC Table 3.10.2.6(B)(16) (base ampacities) from the classpath once, then serves
 * per-material, per-size, per-temperature-rating lookups.
 */
public class AmpacityTable {

	private static final String RESOURCE_PATH = "/reference/electrical/table-3-10-2-6-b-16-ampacity.json";

	private final Map<String, AmpacityEntry> entries;

	public AmpacityTable() {
		this.entries = loadEntries();
	}

	private static Map<String, AmpacityEntry> loadEntries() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = AmpacityTable.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			AmpacityEntry[] rows = objectMapper.readValue(in, AmpacityEntry[].class);
			Map<String, AmpacityEntry> byKey = new HashMap<>();
			for (AmpacityEntry row : rows) {
				byKey.put(key(row.conductorMaterial(), row.sizeLabel(), row.tempRatingCelsius()), row);
			}
			return byKey;
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	private static String key(ConductorMaterial material, String sizeLabel, int tempRatingCelsius) {
		return material.name() + "|" + sizeLabel + "|" + tempRatingCelsius;
	}

	/**
	 * @throws CalculationException if the combination isn't found (e.g. some sizes have no
	 *                               published aluminum ampacity below a minimum branch size)
	 */
	public double lookup(ConductorMaterial material, String sizeLabel, int tempRatingCelsius) {
		AmpacityEntry entry = entries.get(key(material, sizeLabel, tempRatingCelsius));
		if (entry == null) {
			throw new CalculationException("No ampacity data for material " + material
					+ ", size " + sizeLabel + ", temp rating " + tempRatingCelsius + "C");
		}
		return entry.ampacityAmps();
	}

	/**
	 * Distinct sizes with published ampacity data (for any material/temp rating), ascending by
	 * cross-section. Intended for a calculator to "walk up" sizes looking for the smallest one
	 * that satisfies a required ampacity — sizeLabel alone doesn't sort correctly as a string
	 * (e.g. {@code "100"} must sort after {@code "80"} but before {@code "125"}), so this uses
	 * {@link ConductorSize#crossSectionMm2()} for the ordering instead.
	 */
	public List<ConductorSize> allSizesSortedAscendingByArea() {
		Map<String, ConductorSize> byLabel = new HashMap<>();
		for (AmpacityEntry entry : entries.values()) {
			byLabel.computeIfAbsent(entry.sizeLabel(), label -> new ConductorSize(label, Double.parseDouble(label)));
		}
		return byLabel.values().stream()
				.sorted(Comparator.comparingDouble(ConductorSize::crossSectionMm2))
				.toList();
	}

}
