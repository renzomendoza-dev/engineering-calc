package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc.core.electrical.motorflc.MotorFlcInput;
import com.renzoproject.calc.core.electrical.motorflc.MotorFlcResult;
import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc_api.common.EnumParsing;

/**
 * Maps between calc-api's motor FLC DTOs and calc-core's calculator types.
 *
 * <p>{@code phaseType} / {@code motorClass} are plain strings mapped explicitly rather than
 * binding Jackson directly to the enum types -- see {@link EnumParsing} for why.
 */
public final class MotorFlcMapper {

	private MotorFlcMapper() {
	}

	public static MotorFlcInput toInput(MotorFlcRequest request) {
		MotorPhaseType phaseType = EnumParsing.parse(MotorPhaseType.class, request.phaseType(), "motor phase type");
		MotorClass motorClass = request.motorClass() == null
				? null
				: EnumParsing.parse(MotorClass.class, request.motorClass(), "motor class");
		return new MotorFlcInput(
				phaseType,
				motorClass,
				request.horsepowerLabel(),
				request.voltage(),
				request.synchronousPowerFactorPercent());
	}

	public static MotorFlcResponse toResponse(MotorFlcResult result) {
		return MotorFlcResponse.from(result);
	}

}
