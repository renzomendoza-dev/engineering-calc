package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.electrical.voltagedrop.CircuitType;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves conductor resistance/reactance from PEC reference tables, ready to feed into a
 * calculator's input (e.g. {@code VoltageDropInput}). This is a pre-step that runs before any
 * calculator — it does no calculation of its own.
 */
public class ConductorPropertiesResolver {

	private final ConductorDcResistanceTable dcResistanceTable = new ConductorDcResistanceTable();
	private final ConductorImpedanceTable impedanceTable = new ConductorImpedanceTable();

	/**
	 * @param circuitType     DC circuits use {@link ConductorDcResistanceTable} with reactance
	 *                        forced to {@code 0}; AC circuits (single- or three-phase — this
	 *                        resolver doesn't distinguish, that's the calculator's concern) use
	 *                        {@link ConductorImpedanceTable}
	 * @param conduitMaterial ignored for DC lookups, but required for API consistency across
	 *                        both circuit types
	 */
	public ConductorProperties resolve(CircuitType circuitType, ConductorSize size, ConductorMaterial material, ConduitMaterial conduitMaterial) {
		if (circuitType == CircuitType.DC) {
			double resistance = dcResistanceTable.lookupOhmsPerMeter(size, material, false);
			return new ConductorProperties(resistance, 0.0);
		}

		ConductorImpedance impedance = impedanceTable.lookup(size, material, conduitMaterial);
		return new ConductorProperties(impedance.resistanceOhmsPerMeter(), impedance.reactanceOhmsPerMeter());
	}

	/**
	 * All known conductor sizes, unioned across both the AC impedance and DC resistance
	 * tables (deduped by label — some sizes appear in both), ascending by cross-section.
	 * Intended for populating a caller's "known sizes" listing (e.g. a frontend dropdown).
	 */
	public List<ConductorSize> allSizes() {
		Map<String, ConductorSize> byLabel = new LinkedHashMap<>();
		for (ConductorSize size : impedanceTable.allSizes()) {
			byLabel.put(size.label(), size);
		}
		for (ConductorSize size : dcResistanceTable.allSizes()) {
			byLabel.putIfAbsent(size.label(), size);
		}
		return byLabel.values().stream()
				.sorted(Comparator.comparingDouble(ConductorSize::crossSectionMm2))
				.toList();
	}

	/** Raw rows of PEC Table 10.1.1.9 (AC resistance and reactance), for display purposes. */
	public List<ConductorImpedanceEntry> allImpedanceEntries() {
		return impedanceTable.allEntries();
	}

	/** Raw rows of PEC Table 10.1.1.8 (DC resistance), for display purposes. */
	public List<ConductorDcResistanceEntry> allDcResistanceEntries() {
		return dcResistanceTable.allEntries();
	}

}
