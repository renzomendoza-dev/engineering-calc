package com.renzoproject.calc.core.smokecontrol.naturalventing;

import java.util.List;

/**
 * Top-level shape of {@code reference/smoke-control/natural-venting-reference.json}.
 * Package-private: internal to {@link JsonNaturalVentingReferenceResolver}.
 */
record NaturalVentingReferenceFile(
		String standard,
		String edition,
		String confidence,
		String note,
		List<VentDischargeCoefficientRow> dischargeCoefficients,
		List<CommodityHeatReleaseRow> commodities) {

}
