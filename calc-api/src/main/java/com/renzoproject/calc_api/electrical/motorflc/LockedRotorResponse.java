package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc.core.electrical.motorflc.LockedRotorResult;

/**
 * HTTP response body for a locked-rotor current lookup, mirroring calc-core's
 * {@link LockedRotorResult}.
 */
public record LockedRotorResponse(double lockedRotorAmps) {

	public static LockedRotorResponse from(LockedRotorResult result) {
		return new LockedRotorResponse(result.lockedRotorAmps());
	}

}
