package com.renzoproject.calc_api.electrical.voltagedrop;

import com.renzoproject.calc.core.electrical.voltagedrop.VoltageDropResult;

/**
 * HTTP response body for a voltage drop calculation, mirroring calc-core's
 * {@link VoltageDropResult}.
 */
public record VoltageDropResponse(
		double voltageDropVolts,
		double voltageDropPercent,
		double receivingEndVoltage,
		boolean exceedsRecommendedLimit) {

	public static VoltageDropResponse from(VoltageDropResult result) {
		return new VoltageDropResponse(
				result.voltageDropVolts(),
				result.voltageDropPercent(),
				result.receivingEndVoltage(),
				result.exceedsRecommendedLimit());
	}

}
