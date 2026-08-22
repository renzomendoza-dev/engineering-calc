package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.storage.OccupancyTypeRow;

/**
 * HTTP response representation of one row of reference/storage/lpcd-consumption.json, for
 * populating a frontend reference table — display only, not used in any calculation path.
 */
public record OccupancyTypeEntryDto(String type, String label, double lpcd) {

	public static OccupancyTypeEntryDto from(OccupancyTypeRow row) {
		return new OccupancyTypeEntryDto(row.type(), row.label(), row.lpcd());
	}

}
