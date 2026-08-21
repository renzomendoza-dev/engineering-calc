package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.AmpacityEntry;

/**
 * HTTP response representation of one row of PEC Table 3.10.2.6(B)(16) (base ampacities),
 * for populating a frontend reference table. conductorMaterial is serialized as its enum
 * name — display only, not used in any calculation path.
 */
public record AmpacityEntryDto(
		String conductorMaterial,
		String sizeLabel,
		int tempRatingCelsius,
		double ampacityAmps) {

	public static AmpacityEntryDto from(AmpacityEntry entry) {
		return new AmpacityEntryDto(
				entry.conductorMaterial().name(),
				entry.sizeLabel(),
				entry.tempRatingCelsius(),
				entry.ampacityAmps());
	}

}
