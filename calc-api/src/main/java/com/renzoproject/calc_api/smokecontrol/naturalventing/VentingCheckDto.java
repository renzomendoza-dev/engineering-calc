package com.renzoproject.calc_api.smokecontrol.naturalventing;

import com.renzoproject.calc.core.smokecontrol.naturalventing.VentingCheck;

/**
 * One compliance line, mirroring calc-core's {@code VentingCheck}. {@code basis} is
 * {@code NFPA_204} or {@code SUPPLEMENTARY}; {@code status} is {@code PASS}, {@code FAIL},
 * {@code CONFIRM}, {@code OPEN} or {@code INFO}.
 */
public record VentingCheckDto(
		String clause,
		String description,
		String basis,
		String status,
		Double actual,
		Double limit,
		String unit,
		String note) {

	public static VentingCheckDto from(VentingCheck check) {
		return new VentingCheckDto(check.clause(), check.description(), check.basis().name(), check.status().name(),
				check.actual(), check.limit(), check.unit(), check.note());
	}

}
