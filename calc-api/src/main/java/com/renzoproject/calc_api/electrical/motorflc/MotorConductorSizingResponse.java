package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc.core.electrical.motorflc.MotorConductorSizingResult;
import com.renzoproject.calc_api.electrical.wiresizing.WireSizingResponse;

/**
 * HTTP response body for the motor-to-wire-sizing pipeline, mirroring calc-core's
 * {@link MotorConductorSizingResult}. Reuses {@link WireSizingResponse} directly from the
 * wiresizing package rather than redefining an equivalent type here.
 */
public record MotorConductorSizingResponse(MotorFlcResponse motorFlcResult, WireSizingResponse wireSizingResult) {

	public static MotorConductorSizingResponse from(MotorConductorSizingResult result) {
		return new MotorConductorSizingResponse(
				MotorFlcResponse.from(result.motorFlcResult()),
				WireSizingResponse.from(result.wireSizingResult()));
	}

}
