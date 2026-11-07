package com.renzoproject.calc_api.mechanical.standpipe;

import com.renzoproject.calc.core.mechanical.standpipe.StandpipeCheck;

/**
 * One compliance line, mirroring calc-core StandpipeCheck. {@code status} is PASS, FAIL, CONFIRM,
 * OPEN or INFO.
 */
public record StandpipeCheckDto(
		String clause,
		String description,
		String status,
		Double actual,
		Double limit,
		String unit,
		String note) {

	public static StandpipeCheckDto from(StandpipeCheck check) {
		return new StandpipeCheckDto(check.clause(), check.description(), check.status().name(), check.actual(), check.limit(),
				check.unit(), check.note());
	}

}
