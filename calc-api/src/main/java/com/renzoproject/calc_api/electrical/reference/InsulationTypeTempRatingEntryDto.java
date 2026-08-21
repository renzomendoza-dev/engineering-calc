package com.renzoproject.calc_api.electrical.reference;

import com.renzoproject.calc.core.electrical.reference.InsulationTypeTempRatingEntry;

/**
 * HTTP response representation of one row of the insulation-type-to-temperature-rating
 * mapping, for populating a frontend reference table. insulationType is serialized as its
 * published label (e.g. {@code "THHN"}), conductorMaterial as its enum name — display only,
 * not used in any calculation path.
 */
public record InsulationTypeTempRatingEntryDto(
		String insulationType,
		String conductorMaterial,
		int tempRatingCelsius) {

	public static InsulationTypeTempRatingEntryDto from(InsulationTypeTempRatingEntry entry) {
		return new InsulationTypeTempRatingEntryDto(
				entry.insulationType().toLabel(),
				entry.conductorMaterial().name(),
				entry.tempRatingCelsius());
	}

}
