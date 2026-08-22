package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.duct.DuctVelocityLimitRow;

/**
 * HTTP response representation of one row of reference/duct/duct-velocity-limits.json, for
 * populating a frontend suggested-defaults UI — display only, not used in any calculation path
 * ({@code DuctSizingCalculator} takes {@code maxVelocity} as a direct input, same design as
 * {@code PipeVelocityCalculator}).
 */
public record DuctVelocityLimitEntryDto(
		String ductLocation,
		String label,
		int ncRcRating,
		Double maxVelocityRectangularMps,
		Double maxVelocityRoundMps) {

	public static DuctVelocityLimitEntryDto from(DuctVelocityLimitRow row) {
		return new DuctVelocityLimitEntryDto(row.ductLocation(), row.label(), row.ncRcRating(), row.maxVelocityRectangular(), row.maxVelocityRound());
	}

}
