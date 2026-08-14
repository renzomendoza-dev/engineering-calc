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
 * Loads PEC Table 10.1.1.8 (DC resistance) from the classpath once, then serves per-size,
 * per-material lookups converted to ohms/meter.
 *
 * <p>Some sizes publish both a solid and a stranded row; lookups default to the stranded row
 * (typical of building wire) and only fall back to solid when that's the only row available.
 */
public class ConductorDcResistanceTable {

	private static final String RESOURCE_PATH = "/reference/electrical/table-10-1-1-8.json";
	private static final double OHMS_PER_305_METERS_TO_PER_METER = 305.0;

	private final Map<String, List<ConductorDcResistanceEntry>> entriesBySize;

	public ConductorDcResistanceTable() {
		this.entriesBySize = loadEntries();
	}

	private static Map<String, List<ConductorDcResistanceEntry>> loadEntries() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = ConductorDcResistanceTable.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			ConductorDcResistanceEntry[] entries = objectMapper.readValue(in, ConductorDcResistanceEntry[].class);
			Map<String, List<ConductorDcResistanceEntry>> bySize = new HashMap<>();
			for (ConductorDcResistanceEntry entry : entries) {
				bySize.computeIfAbsent(entry.sizeLabel(), key -> new ArrayList<>()).add(entry);
			}
			return bySize;
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	/**
	 * @throws CalculationException if the size has no table entry
	 */
	public double lookupOhmsPerMeter(ConductorSize size, ConductorMaterial material, boolean coated) {
		List<ConductorDcResistanceEntry> candidates = entriesBySize.get(size.label());
		if (candidates == null || candidates.isEmpty()) {
			throw new CalculationException("No DC resistance data for conductor size " + size.label());
		}

		ConductorDcResistanceEntry entry = candidates.stream()
				.filter(ConductorDcResistanceEntry::stranded)
				.findFirst()
				.orElse(candidates.get(0));

		double resistance = material == ConductorMaterial.COPPER
				? (coated ? entry.copperCoatedOhmsPer305m() : entry.copperUncoatedOhmsPer305m())
				: entry.aluminumOhmsPer305m();

		return resistance / OHMS_PER_305_METERS_TO_PER_METER;
	}

	/**
	 * All conductor sizes with published DC resistance data, ascending by cross-section.
	 */
	public List<ConductorSize> allSizes() {
		return entriesBySize.keySet().stream()
				.map(label -> new ConductorSize(label, Double.parseDouble(label)))
				.sorted(Comparator.comparingDouble(ConductorSize::crossSectionMm2))
				.toList();
	}

	/**
	 * All raw table rows, as published (ohms per 305 meters, not converted), ascending by
	 * cross-section then solid-before-stranded. Intended for displaying the table itself
	 * (e.g. a frontend reference table), as opposed to {@link #lookupOhmsPerMeter} which
	 * resolves a single size/material combination to ohms/meter for a calculation.
	 */
	public List<ConductorDcResistanceEntry> allEntries() {
		return entriesBySize.values().stream()
				.flatMap(List::stream)
				.sorted(Comparator
						.comparingDouble((ConductorDcResistanceEntry entry) -> Double.parseDouble(entry.sizeLabel()))
						.thenComparing(ConductorDcResistanceEntry::stranded))
				.toList();
	}

}
