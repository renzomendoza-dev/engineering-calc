package com.renzoproject.calc_api.smokecontrol.naturalventing;

import java.util.List;

/**
 * The NFPA 204 tables the natural venting form offers as defaults, with the edition they were
 * read from. Units: kW per m2 for the commodity rates; the discharge coefficients have none.
 */
public record NaturalVentingReferenceResponse(
		String edition,
		String note,
		List<DischargeCoefficientEntryDto> dischargeCoefficients,
		List<CommodityHeatReleaseEntryDto> commodities) {

}
