package com.renzoproject.calc.core.mechanical.standpipe;

/**
 * Outcome of one {@link StandpipeCheck}.
 */
public enum CheckStatus {

	/** Requirement met. */
	PASS,

	/** Requirement not met. */
	FAIL,

	/** Depends on something the designer must confirm. */
	CONFIRM,

	/** Cannot be assessed: an input it needs was not given. */
	OPEN,

	/** Reported for information. */
	INFO

}
