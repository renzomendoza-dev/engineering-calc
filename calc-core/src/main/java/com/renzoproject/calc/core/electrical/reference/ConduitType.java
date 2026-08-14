package com.renzoproject.calc.core.electrical.reference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.renzoproject.calc.core.exception.CalculationException;

import java.util.HashMap;
import java.util.Map;

/**
 * Conduit/tubing product type, as published in PEC Table 10.1.1.4. Distinct from
 * {@link ConduitMaterial} (used by voltage drop for impedance lookups) — this represents the
 * physical conduit product for fill calculations, not a material category for impedance.
 *
 * <p>{@link #toLabel()} / {@link #fromLabel(String)} round-trip to the original published
 * label (e.g. {@code "LFNC-A"} -> {@link #LFNC_A}) for JSON (de)serialization.
 */
public enum ConduitType {
	EMT("EMT"),
	ENT("ENT"),
	FMC("FMC"),
	IMC("IMC"),
	LFNC_A("LFNC-A"),
	LFNC_B("LFNC-B"),
	LFNC_C("LFNC-C"),
	LFMC("LFMC"),
	RMC("RMC"),
	PVC_SCHEDULE_80("PVC_SCHEDULE_80"),
	PVC_SCHEDULE_40_HDPE("PVC_SCHEDULE_40_HDPE"),
	PVC_TYPE_A("PVC_TYPE_A"),
	PVC_TYPE_EB("PVC_TYPE_EB");

	private static final Map<String, ConduitType> BY_LABEL = new HashMap<>();

	static {
		for (ConduitType type : values()) {
			BY_LABEL.put(type.label, type);
		}
	}

	private final String label;

	ConduitType(String label) {
		this.label = label;
	}

	@JsonValue
	public String toLabel() {
		return label;
	}

	@JsonCreator
	public static ConduitType fromLabel(String label) {
		ConduitType type = BY_LABEL.get(label);
		if (type == null) {
			throw new CalculationException("Unknown conduit type: " + label);
		}
		return type;
	}

}
