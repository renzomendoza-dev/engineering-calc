package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc.core.electrical.motorflc.MotorFlcResult;

/**
 * HTTP response body for a motor full-load current lookup, mirroring calc-core's
 * {@link MotorFlcResult}.
 */
public record MotorFlcResponse(double flcAmps, double baseFlcAmps, double minimumConductorAmpacity) {

	public static MotorFlcResponse from(MotorFlcResult result) {
		return new MotorFlcResponse(result.flcAmps(), result.baseFlcAmps(), result.minimumConductorAmpacity());
	}

}
