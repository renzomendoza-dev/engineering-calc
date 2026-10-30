package com.renzoproject.calc.core.smokecontrol.naturalventing;

/**
 * Outcome of one {@link VentingCheck}.
 */
public enum CheckStatus {

	/** Requirement met. */
	PASS,

	/** Requirement not met. */
	FAIL,

	/** Met or not depending on a value the designer must confirm (an assumed input). */
	CONFIRM,

	/** Cannot be assessed: an input it needs was not given. */
	OPEN,

	/** Reported for information; no pass/fail criterion. */
	INFO

}
