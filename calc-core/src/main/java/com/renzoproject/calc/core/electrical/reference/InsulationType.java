package com.renzoproject.calc.core.electrical.reference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.renzoproject.calc.core.exception.CalculationException;

import java.util.HashMap;
import java.util.Map;

/**
 * Conductor insulation type, as published in PEC Table 10.1.1.5. Enum constant names replace
 * hyphens in the published label with underscores (e.g. {@code "RFH-2"} -> {@link #RFH_2}) to
 * form valid Java identifiers; {@link #toLabel()} / {@link #fromLabel(String)} round-trip to
 * the original label for JSON (de)serialization.
 */
public enum InsulationType {
	RFH_2("RFH-2"),
	FFH_2("FFH-2"),
	RFHH_2("RFHH-2"),
	RHH("RHH"),
	RHW("RHW"),
	RHW_2("RHW-2"),
	SF_2("SF-2"),
	SFF_2("SFF-2"),
	SF_1("SF-1"),
	SFF_1("SFF-1"),
	RFH_1("RFH-1"),
	TF("TF"),
	TFF("TFF"),
	XF("XF"),
	XFF("XFF"),
	TW("TW"),
	THHW("THHW"),
	THW("THW"),
	THW_2("THW-2"),
	TFN("TFN"),
	TFFN("TFFN"),
	THHN("THHN"),
	THWN("THWN"),
	THWN_2("THWN-2"),
	PF("PF"),
	PGFF("PGFF"),
	PGF("PGF"),
	PFF("PFF"),
	PTF("PTF"),
	PAF("PAF"),
	PTFF("PTFF"),
	PAFF("PAFF"),
	TFE("TFE"),
	FEP("FEP"),
	PFA("PFA"),
	FEPB("FEPB"),
	PFAH("PFAH"),
	ZF("ZF"),
	ZFF("ZFF"),
	ZHF("ZHF"),
	Z("Z"),
	XHHW("XHHW"),
	ZW("ZW"),
	XHHW_2("XHHW-2"),
	XHH("XHH"),
	KF_2("KF-2"),
	KFF_2("KFF-2"),
	KF_1("KF-1"),
	KFF_1("KFF-1"),
	MI("MI"),
	SA("SA"),
	SIS("SIS"),
	TBS("TBS"),
	UF("UF"),
	USE("USE"),
	USE_2("USE-2"),
	ZW_2("ZW-2");

	private static final Map<String, InsulationType> BY_LABEL = new HashMap<>();

	static {
		for (InsulationType type : values()) {
			BY_LABEL.put(type.label, type);
		}
	}

	private final String label;

	InsulationType(String label) {
		this.label = label;
	}

	@JsonValue
	public String toLabel() {
		return label;
	}

	@JsonCreator
	public static InsulationType fromLabel(String label) {
		InsulationType type = BY_LABEL.get(label);
		if (type == null) {
			throw new CalculationException("Unknown insulation type: " + label);
		}
		return type;
	}

}
