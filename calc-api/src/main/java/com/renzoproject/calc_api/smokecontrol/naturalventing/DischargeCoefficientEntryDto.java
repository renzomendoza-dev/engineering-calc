package com.renzoproject.calc_api.smokecontrol.naturalventing;

/** One default discharge coefficient for a vent or inlet type (NFPA 204 Table 9.2.4.2 or A.9.2.4.2). */
public record DischargeCoefficientEntryDto(
		String key,
		String label,
		double dischargeCoefficient,
		String clause,
		String confidence) {

}
