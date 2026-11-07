package com.renzoproject.calc.core.smokecontrol.naturalventing;

import java.util.List;

/**
 * The NFPA 204 reference tables a natural venting form can offer as defaults: the discharge
 * coefficients of Table 9.2.4.2 and the unit heat release rates of Table A.8.2.6. The calculator
 * itself takes plain numbers, so it does not use this.
 */
public interface NaturalVentingReferenceResolver {

	List<VentDischargeCoefficientRow> allDischargeCoefficients();

	List<CommodityHeatReleaseRow> allCommodities();

	/** Edition and confidence note of the file, for display next to the tables. */
	String edition();

	String note();

}
