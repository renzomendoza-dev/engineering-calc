package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.LockedRotorEntry;

/**
 * HTTP response representation of one row from either PEC Table 4.30.14.5(A) (single-phase)
 * or 4.30.14.5(B) (polyphase) locked-rotor current tables, for populating a frontend
 * reference table. Shared by both — the endpoint the caller hit determines which source
 * table these rows came from. Display only, not used in any calculation path.
 */
public record LockedRotorEntryDto(String sizeLabel, int voltage, double lockedRotorAmps) {

	public static LockedRotorEntryDto from(LockedRotorEntry entry) {
		return new LockedRotorEntryDto(entry.sizeLabel(), entry.voltage(), entry.lockedRotorAmps());
	}

}
