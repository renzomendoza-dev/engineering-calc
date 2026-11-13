package com.renzoproject.calc.core.compliance;

/**
 * Outcome of one {@link ComplianceCheck}, shared by every calculator that reports a compliance
 * table.
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
