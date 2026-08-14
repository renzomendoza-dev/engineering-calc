package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.MotorFlcEntry;

/**
 * HTTP response representation of one row from the combined PEC Article 4.30 full-load
 * current tables (4.30.14.1 DC, 4.30.14.2 single-phase, 4.30.14.3 two-phase, 4.30.14.4
 * three-phase), for populating a frontend reference table. phaseType/motorClass are
 * serialized as their enum names (motorClass is null except for three-phase rows) — display
 * only, not used in any calculation path.
 */
public record MotorFlcEntryDto(
		String phaseType,
		String motorClass,
		String sizeLabel,
		int voltage,
		double flcAmps) {

	public static MotorFlcEntryDto from(MotorFlcEntry entry) {
		return new MotorFlcEntryDto(
				entry.phaseType().name(),
				entry.motorClass() == null ? null : entry.motorClass().name(),
				entry.sizeLabel(),
				entry.voltage(),
				entry.flcAmps());
	}

}
