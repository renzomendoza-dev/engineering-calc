package com.renzoproject.calc.core.mechanical.standpipe;

/**
 * What a pipe segment is, for the minimum-size rules of 7.6 (which apply to Class I and Class III).
 */
public enum PipeRole {

	/** Piping from a hose valve to the standpipe (8.3.1.2). No minimum size is checked. */
	HOSE_PIPING,

	/** The standpipe itself: at least 4 in. (7.6.1), 6 in. in a combined partly sprinklered building (7.6.2). */
	STANDPIPE,

	/** A branch line: at least 2.5 in. (7.6.4). */
	BRANCH_LINE,

	/** Supply piping and anything else. No minimum size is checked. */
	OTHER

}
