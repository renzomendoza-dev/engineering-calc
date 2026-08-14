package com.renzoproject.calc.core.electrical.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads all four PEC Article 4.30 full-load current tables (4.30.14.1 DC, 4.30.14.2
 * single-phase, 4.30.14.3 two-phase, 4.30.14.4 three-phase) from the classpath once, combined
 * into a single lookup.
 */
public class MotorFlcTable {

	private static final String DC_RESOURCE_PATH = "/reference/electrical/motor-flc-dc.json";
	private static final String SINGLE_PHASE_RESOURCE_PATH = "/reference/electrical/motor-flc-single-phase.json";
	private static final String TWO_PHASE_RESOURCE_PATH = "/reference/electrical/motor-flc-two-phase.json";
	private static final String THREE_PHASE_RESOURCE_PATH = "/reference/electrical/motor-flc-three-phase.json";

	private final Map<String, MotorFlcEntry> entries;

	public MotorFlcTable() {
		Map<String, MotorFlcEntry> combined = new HashMap<>();
		loadInto(combined, DC_RESOURCE_PATH);
		loadInto(combined, SINGLE_PHASE_RESOURCE_PATH);
		loadInto(combined, TWO_PHASE_RESOURCE_PATH);
		loadInto(combined, THREE_PHASE_RESOURCE_PATH);
		this.entries = combined;
	}

	private static void loadInto(Map<String, MotorFlcEntry> target, String resourcePath) {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = MotorFlcTable.class.getResourceAsStream(resourcePath)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + resourcePath);
			}
			MotorFlcEntry[] rows = objectMapper.readValue(in, MotorFlcEntry[].class);
			for (MotorFlcEntry row : rows) {
				target.put(key(row.phaseType(), row.motorClass(), row.sizeLabel(), row.voltage()), row);
			}
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + resourcePath, e);
		}
	}

	private static String key(MotorPhaseType phaseType, MotorClass motorClass, String sizeLabel, int voltage) {
		return phaseType.name() + "|" + (motorClass == null ? "" : motorClass.name()) + "|" + sizeLabel + "|" + voltage;
	}

	/**
	 * @param motorClass ignored (normalized to {@code null}) unless {@code phaseType} is
	 *                   {@link MotorPhaseType#THREE_PHASE} — only three-phase data
	 *                   distinguishes induction vs. synchronous
	 * @throws CalculationException if the combination isn't published (many HP/voltage cells
	 *                              are blank in the source tables because that pairing isn't
	 *                              standard)
	 */
	public double lookup(MotorPhaseType phaseType, MotorClass motorClass, String sizeLabel, int voltage) {
		MotorClass effectiveMotorClass = phaseType == MotorPhaseType.THREE_PHASE ? motorClass : null;
		MotorFlcEntry entry = entries.get(key(phaseType, effectiveMotorClass, sizeLabel, voltage));
		if (entry == null) {
			throw new CalculationException("No FLC data for phase type " + phaseType
					+ (effectiveMotorClass != null ? ", motor class " + effectiveMotorClass : "")
					+ ", size " + sizeLabel + ", voltage " + voltage + "V");
		}
		return entry.flcAmps();
	}

	/**
	 * Distinct horsepower labels published for the given phase/class combination, ascending
	 * by numeric horsepower (using {@link HorsepowerRating#parseHp} — sizeLabel alone doesn't
	 * sort correctly as a string). Intended for populating a caller's horsepower dropdown with
	 * only valid options for the selected phase/class.
	 *
	 * @param motorClass ignored (normalized to {@code null}) unless {@code phaseType} is
	 *                   {@link MotorPhaseType#THREE_PHASE}, same as {@link #lookup}
	 */
	public List<String> sizeLabelsFor(MotorPhaseType phaseType, MotorClass motorClass) {
		MotorClass effectiveMotorClass = phaseType == MotorPhaseType.THREE_PHASE ? motorClass : null;
		return entries.values().stream()
				.filter(entry -> entry.phaseType() == phaseType && Objects.equals(entry.motorClass(), effectiveMotorClass))
				.map(MotorFlcEntry::sizeLabel)
				.distinct()
				.sorted(Comparator.comparingDouble(HorsepowerRating::parseHp))
				.toList();
	}

	/**
	 * Distinct voltages published for the given phase/class/size combination, ascending.
	 * Intended for populating a caller's voltage dropdown with only valid options once a
	 * horsepower has been selected.
	 *
	 * @param motorClass ignored (normalized to {@code null}) unless {@code phaseType} is
	 *                   {@link MotorPhaseType#THREE_PHASE}, same as {@link #lookup}
	 */
	public List<Integer> voltagesFor(MotorPhaseType phaseType, MotorClass motorClass, String sizeLabel) {
		MotorClass effectiveMotorClass = phaseType == MotorPhaseType.THREE_PHASE ? motorClass : null;
		return entries.values().stream()
				.filter(entry -> entry.phaseType() == phaseType
						&& Objects.equals(entry.motorClass(), effectiveMotorClass)
						&& entry.sizeLabel().equals(sizeLabel))
				.map(MotorFlcEntry::voltage)
				.distinct()
				.sorted()
				.toList();
	}

	/**
	 * All raw rows from all four combined source tables, as published, ascending by phase
	 * type, then motor class (induction/unspecified before synchronous), then horsepower,
	 * then voltage. Intended for displaying the table itself (e.g. a frontend reference
	 * table), as opposed to {@link #lookup} which resolves a single combination.
	 */
	public List<MotorFlcEntry> allEntries() {
		return entries.values().stream()
				.sorted(Comparator
						.comparing((MotorFlcEntry entry) -> entry.phaseType().name())
						.thenComparing(entry -> entry.motorClass() == null ? "" : entry.motorClass().name())
						.thenComparingDouble(entry -> HorsepowerRating.parseHp(entry.sizeLabel()))
						.thenComparingInt(MotorFlcEntry::voltage))
				.toList();
	}

}
