package com.renzoproject.calc_api.compliance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.renzoproject.calc.core.compliance.ComplianceCheck;

/**
 * One compliance line, mirroring calc-core's {@code ComplianceCheck}. {@code status} is
 * {@code PASS}, {@code FAIL}, {@code CONFIRM}, {@code OPEN} or {@code INFO}. {@code basis} is
 * {@code NFPA_204} or {@code SUPPLEMENTARY} for the calculators that label their checks that way
 * (natural venting) and is left out of the JSON for the rest.
 */
public record ComplianceCheckDto(
		String clause,
		String description,
		@JsonInclude(JsonInclude.Include.NON_NULL) String basis,
		String status,
		Double actual,
		Double limit,
		String unit,
		String note) {

	public static ComplianceCheckDto from(ComplianceCheck check) {
		return new ComplianceCheckDto(check.clause(), check.description(),
				check.basis() == null ? null : check.basis().name(), check.status().name(), check.actual(),
				check.limit(), check.unit(), check.note());
	}

}
