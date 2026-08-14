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
 * Loads PEC Table 4.30.14.5(B) (polyphase locked-rotor current, for selecting disconnecting
 * means and controllers) from the classpath once, then serves per-size, per-voltage lookups.
 *
 * <p>The source table publishes one set of values shared by Design B, C, and D motors — Design
 * A has no published locked-rotor current limit and so is intentionally absent from the data.
 *
 * <p>This is a separate calculation purpose from FLC (used for conductor/branch-circuit
 * sizing) — deliberately kept in its own class rather than merged with {@link MotorFlcTable}.
 */
public class LockedRotorPolyphaseTable {

	private static final String RESOURCE_PATH = "/reference/electrical/locked-rotor-polyphase.json";

	private final Map<String, LockedRotorEntry> entries;

	public LockedRotorPolyphaseTable() {
		this.entries = loadEntries();
	}

	private static Map<String, LockedRotorEntry> loadEntries() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = LockedRotorPolyphaseTable.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			LockedRotorEntry[] rows = objectMapper.readValue(in, LockedRotorEntry[].class);
			Map<String, LockedRotorEntry> byKey = new HashMap<>();
			for (LockedRotorEntry row : rows) {
				byKey.put(key(row.sizeLabel(), row.voltage()), row);
			}
			return byKey;
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	private static String key(String sizeLabel, int voltage) {
		return sizeLabel + "|" + voltage;
	}

	/**
	 * @throws CalculationException if the combination isn't found
	 */
	public double lookup(String sizeLabel, int voltage) {
		LockedRotorEntry entry = entries.get(key(sizeLabel, voltage));
		if (entry == null) {
			throw new CalculationException(
					"No polyphase locked-rotor data for size " + sizeLabel + ", voltage " + voltage + "V");
		}
		return entry.lockedRotorAmps();
	}

	/**
	 * All raw rows, as published, ascending by horsepower then voltage. Intended for
	 * displaying the table itself (e.g. a frontend reference table), as opposed to
	 * {@link #lookup} which resolves a single size/voltage combination.
	 */
	public List<LockedRotorEntry> allEntries() {
		return entries.values().stream()
				.sorted(Comparator
						.comparingDouble((LockedRotorEntry entry) -> HorsepowerRating.parseHp(entry.sizeLabel()))
						.thenComparingInt(LockedRotorEntry::voltage))
				.toList();
	}

}
