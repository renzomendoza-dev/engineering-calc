package com.renzoproject.calc.core.electrical.reference;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PEC 2.40.1.4(D) Small Conductors: the largest overcurrent device that may protect a conductor of
 * 5.5 mm2 or less. Larger sizes have no such limit, and ampacity and 2.40.1.4(B) govern.
 */
public class SmallConductorLimitTable {

	private static final String RESOURCE_PATH = "/reference/electrical/small-conductor-limits.json";

	private final SmallConductorLimitFile file = ReferenceJson.read(RESOURCE_PATH, SmallConductorLimitFile.class);
	private final Map<String, SmallConductorLimitEntry> byKey = new HashMap<>();

	public SmallConductorLimitTable() {
		for (SmallConductorLimitEntry entry : file.limits()) {
			byKey.put(key(entry.conductorMaterial(), entry.sizeLabel()), entry);
		}
	}

	private static String key(ConductorMaterial material, String sizeLabel) {
		return material.name() + "|" + sizeLabel;
	}

	/** The limit for this size and material; empty for a size the clause does not cover. */
	public Optional<SmallConductorLimitEntry> lookup(ConductorMaterial material, String sizeLabel) {
		return Optional.ofNullable(byKey.get(key(material, sizeLabel)));
	}

	/** Every printed line, in the order the PEC lists them. */
	public List<SmallConductorLimitEntry> allEntries() {
		return file.limits();
	}

	/** Where the table was read from and how far to trust it, for display next to it. */
	public String standard() {
		return file.standard();
	}

	public String note() {
		return file.note();
	}

}
