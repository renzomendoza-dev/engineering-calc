package com.renzoproject.calc.core;

/**
 * Common contract for calc-core calculators.
 *
 * @param <I> input type, typically a validated record
 * @param <R> result type
 */
public interface Calculator<I, R> {

	/**
	 * @param input validated calculation input
	 * @return the calculation result
	 */
	R calculate(I input);

}
