package com.renzoproject.calc.core.smokecontrol.naturalventing;

/**
 * How the design heat release rate of a {@link FireCase} is obtained (NFPA 204, Chapter 8).
 */
public enum FireCaseKind {

	/** Steady (limited-growth) fire: the user gives the heat release rate directly (8.2). */
	STEADY,

	/** Continuously growing t-squared fire, taken at the end of the design interval (8.3.5). */
	GROWING

}
