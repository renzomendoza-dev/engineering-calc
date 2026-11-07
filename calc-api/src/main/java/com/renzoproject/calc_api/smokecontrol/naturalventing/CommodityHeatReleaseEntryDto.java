package com.renzoproject.calc_api.smokecontrol.naturalventing;

/** One row of NFPA 204 Table A.8.2.6, in kW per m2 of floor area. */
public record CommodityHeatReleaseEntryDto(
		String key,
		String label,
		double heatReleaseRateKwPerM2,
		String confidence) {

}
