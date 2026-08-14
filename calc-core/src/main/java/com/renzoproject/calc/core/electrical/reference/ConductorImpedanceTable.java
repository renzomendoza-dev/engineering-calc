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
 * Loads PEC Table 10.1.1.9 (AC resistance and reactance) from the classpath once, then serves
 * per-size, per-material, per-conduit lookups converted to ohms/meter.
 */
public class ConductorImpedanceTable {

	private static final String RESOURCE_PATH = "/reference/table-10-1-1-9.json";
	private static final double OHMS_PER_305_METERS_TO_PER_METER = 305.0;

	private final Map<String, ConductorImpedanceEntry> entriesBySize;

	public ConductorImpedanceTable() {
		this.entriesBySize = loadEntries();
	}

	private static Map<String, ConductorImpedanceEntry> loadEntries() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = ConductorImpedanceTable.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			ConductorImpedanceEntry[] entries = objectMapper.readValue(in, ConductorImpedanceEntry[].class);
			Map<String, ConductorImpedanceEntry> bySize = new HashMap<>();
			for (ConductorImpedanceEntry entry : entries) {
				bySize.put(entry.sizeLabel(), entry);
			}
			return bySize;
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	/**
	 * @throws CalculationException if the size has no table entry, or the material/conduit
	 *                               combination has no published resistance value for it
	 */
	public ConductorImpedance lookup(ConductorSize size, ConductorMaterial material, ConduitMaterial conduitMaterial) {
		ConductorImpedanceEntry entry = entriesBySize.get(size.label());
		if (entry == null) {
			throw new CalculationException("No impedance data for conductor size " + size.label());
		}

		double reactance = conduitMaterial == ConduitMaterial.STEEL
				? entry.reactanceSteelConduitOhmsPer305m()
				: entry.reactancePvcAluminumConduitOhmsPer305m();

		Double resistance = resolveResistance(entry, material, conduitMaterial);
		if (resistance == null) {
			throw new CalculationException("No resistance data for size " + size.label()
					+ ", material " + material + ", conduit " + conduitMaterial);
		}

		return new ConductorImpedance(
				resistance / OHMS_PER_305_METERS_TO_PER_METER,
				reactance / OHMS_PER_305_METERS_TO_PER_METER);
	}

	/**
	 * All conductor sizes with published AC impedance data, ascending by cross-section.
	 */
	public List<ConductorSize> allSizes() {
		return entriesBySize.keySet().stream()
				.map(label -> new ConductorSize(label, Double.parseDouble(label)))
				.sorted(Comparator.comparingDouble(ConductorSize::crossSectionMm2))
				.toList();
	}

	/**
	 * All raw table rows, as published (ohms per 305 meters, not converted), ascending by
	 * cross-section. Intended for displaying the table itself (e.g. a frontend reference
	 * table), as opposed to {@link #lookup} which resolves a single size/material/conduit
	 * combination to ohms/meter for a calculation.
	 */
	public List<ConductorImpedanceEntry> allEntries() {
		return entriesBySize.values().stream()
				.sorted(Comparator.comparingDouble(entry -> Double.parseDouble(entry.sizeLabel())))
				.toList();
	}

	private static Double resolveResistance(ConductorImpedanceEntry entry, ConductorMaterial material, ConduitMaterial conduitMaterial) {
		if (material == ConductorMaterial.COPPER) {
			return switch (conduitMaterial) {
				case PVC -> entry.resistanceCopperPvcOhmsPer305m();
				case ALUMINUM -> entry.resistanceCopperAluminumConduitOhmsPer305m();
				case STEEL -> entry.resistanceCopperSteelOhmsPer305m();
			};
		}
		return switch (conduitMaterial) {
			case PVC -> entry.resistanceAluminumWirePvcOhmsPer305m();
			case ALUMINUM -> entry.resistanceAluminumWireAluminumConduitOhmsPer305m();
			case STEEL -> entry.resistanceAluminumWireSteelOhmsPer305m();
		};
	}

}
