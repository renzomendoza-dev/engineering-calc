package com.renzoproject.calc.core.exception;

/**
 * Thrown when calculator input fails validation or a calculation cannot be completed.
 * Unchecked, since these represent programming/data errors the caller is expected to
 * prevent rather than recover from at each call site.
 */
public class CalculationException extends RuntimeException {

	public CalculationException(String message) {
		super(message);
	}

	public CalculationException(String message, Throwable cause) {
		super(message, cause);
	}

}
