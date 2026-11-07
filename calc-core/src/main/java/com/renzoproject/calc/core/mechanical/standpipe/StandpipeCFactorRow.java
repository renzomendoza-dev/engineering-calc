package com.renzoproject.calc.core.mechanical.standpipe;

/**
 * One row of the Hazen-Williams C table. A default a form can offer; each segment still takes its
 * own C, because the AHJ may require another value.
 *
 * @param confidence {@code verified} or {@code verify}
 */
public record StandpipeCFactorRow(
		String pipeType,
		String label,
		double cFactor,
		String confidence) {

}
