package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc.core.electrical.motorflc.MotorFlcInput;
import com.renzoproject.calc.core.electrical.motorflc.MotorFlcResult;
import com.renzoproject.calc.core.electrical.reference.MotorClass;
import com.renzoproject.calc.core.electrical.reference.MotorPhaseType;
import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Maps between calc-api's motor FLC DTOs and calc-core's calculator types.
 *
 * <p>{@code phaseType} / {@code motorClass} are plain strings mapped explicitly here rather
 * than binding Jackson directly to the enum types — same rationale already documented on wire
 * sizing's {@code WireSizingMapper}: a bad value should surface as a clear "Unknown X: value"
 * {@code CalculationException} → 400 via the existing {@code GlobalExceptionHandler}, not a
 * generic Jackson deserialization failure. Neither enum has a hyphenated label, so the same
 * generic {@link #parseEnum} helper used there is reimplemented here (package-private, not
 * duplicated a second time) so {@link MotorConductorSizingMapper} — which lives in this same
 * package and needs the identical phaseType/motorClass parsing — can call it directly instead
 * of a third copy.
 */
public final class MotorFlcMapper {

	private MotorFlcMapper() {
	}

	public static MotorFlcInput toInput(MotorFlcRequest request) {
		MotorPhaseType phaseType = parseEnum(MotorPhaseType.class, request.phaseType(), "motor phase type");
		MotorClass motorClass = request.motorClass() == null
				? null
				: parseEnum(MotorClass.class, request.motorClass(), "motor class");
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

	static <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, String fieldLabel) {
		try {
			return Enum.valueOf(enumType, rawValue);
		} catch (IllegalArgumentException e) {
			throw new CalculationException("Unknown " + fieldLabel + ": " + rawValue);
		}
	}

}
