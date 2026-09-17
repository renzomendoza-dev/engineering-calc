package com.renzoproject.calc_api.common;

import com.renzoproject.calc.core.exception.CalculationException;

/**
 * Maps a request's plain-string field onto a calc-core enum.
 *
 * <p>Several request DTOs accept enum-ish fields ({@code circuitType}, {@code phaseType},
 * {@code mode}, ...) as strings rather than binding Jackson directly to the enum type, so a bad
 * value surfaces as a specific "Unknown circuit type: FOO" 400 from {@code GlobalExceptionHandler}
 * instead of the generic "malformed request body" message a Jackson conversion failure produces.
 * This is the one implementation of that mapping; mappers and services call it rather than each
 * keeping a private copy.
 */
public final class EnumParsing {

	private EnumParsing() {
	}

	/**
	 * @param enumType   the calc-core enum to map onto
	 * @param rawValue   the request value, expected to match a constant name exactly
	 * @param fieldLabel human-readable field name for the error message, e.g. {@code "circuit type"}
	 * @throws CalculationException if {@code rawValue} is {@code null} or matches no constant.
	 *                              {@code Enum.valueOf} alone would throw
	 *                              {@code NullPointerException} for {@code null}, which would
	 *                              surface as a 500 rather than a 400.
	 */
	public static <E extends Enum<E>> E parse(Class<E> enumType, String rawValue, String fieldLabel) {
		if (rawValue == null) {
			throw new CalculationException(fieldLabel + " is required");
		}
		try {
			return Enum.valueOf(enumType, rawValue);
		} catch (IllegalArgumentException e) {
			throw new CalculationException("Unknown " + fieldLabel + ": " + rawValue);
		}
	}

}
