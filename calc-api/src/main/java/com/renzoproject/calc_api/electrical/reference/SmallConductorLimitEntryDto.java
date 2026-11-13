package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.SmallConductorLimitEntry;

/**
 * One line of PEC 2.40.1.4(D) Small Conductors, for the frontend reference table. conductorMaterial
 * is the enum name; ALUMINUM also stands for copper-clad aluminum. conditions is null for a line
 * with none. Display only, not used in any calculation path.
 */
public record SmallConductorLimitEntryDto(
		String conductorMaterial,
		String sizeLabel,
		int maximumOvercurrentDeviceAmps,
		String conditions) {

	public static SmallConductorLimitEntryDto from(SmallConductorLimitEntry entry) {
		return new SmallConductorLimitEntryDto(entry.conductorMaterial().name(), entry.sizeLabel(),
				entry.maximumOvercurrentDeviceAmps(), entry.conditions());
	}

}
