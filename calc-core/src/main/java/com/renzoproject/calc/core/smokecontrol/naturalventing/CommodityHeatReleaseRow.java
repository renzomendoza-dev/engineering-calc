package com.renzoproject.calc.core.smokecontrol.naturalventing;

/**
 * One row of NFPA 204 Table A.8.2.6: the heat release rate per unit floor area of a fully involved
 * commodity, at the stack height named in the label. Used to find the fire diameter from the heat
 * release rate (8.3.7).
 *
 * @param key                    stable identifier of the commodity
 * @param label                  the commodity and its stack height as written in the table
 * @param heatReleaseRateKwPerM2 kW per m2 of floor area
 * @param confidence             {@code verified} or {@code verify}
 */
public record CommodityHeatReleaseRow(
		String key,
		String label,
		double heatReleaseRateKwPerM2,
		String confidence) {

}
