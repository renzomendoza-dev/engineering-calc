package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc.core.electrical.motorflc.LockedRotorInput;
import com.renzoproject.calc.core.electrical.motorflc.LockedRotorResult;

/**
 * Maps between calc-api's locked-rotor DTOs and calc-core's calculator types. No enum fields
 * here, so unlike {@link MotorFlcMapper} there's no label parsing to do.
 */
public final class LockedRotorMapper {

	private LockedRotorMapper() {
	}

	public static LockedRotorInput toInput(LockedRotorRequest request) {
		return new LockedRotorInput(request.isPolyphase(), request.horsepowerLabel(), request.voltage());
	}

	public static LockedRotorResponse toResponse(LockedRotorResult result) {
		return LockedRotorResponse.from(result);
	}

}
