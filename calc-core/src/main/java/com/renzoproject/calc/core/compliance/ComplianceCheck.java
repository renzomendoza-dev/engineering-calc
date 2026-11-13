package com.renzoproject.calc.core.compliance;

/**
 * One line of a compliance table: a clause, what was checked, the figures compared and the
 * outcome.
 *
 * @param clause      the standard's clause or annex reference, e.g. {@code "5.4.1.1"}
 * @param description what is checked, in words
 * @param basis       whether the clause is a requirement of the standard; {@code null} when the
 *                    calculator does not make the distinction
 * @param status      outcome
 * @param actual      the design value; nullable, in particular when {@link CheckStatus#OPEN}
 * @param limit       the value it is compared with; nullable when there is none
 * @param unit        unit of {@code actual} and {@code limit}; nullable
 * @param note        extra explanation; nullable
 */
public record ComplianceCheck(
		String clause,
		String description,
		CheckBasis basis,
		CheckStatus status,
		Double actual,
		Double limit,
		String unit,
		String note) {

	/** A line of a calculator that does not label its checks by basis. */
	public ComplianceCheck(String clause, String description, CheckStatus status, Double actual, Double limit, String unit,
			String note) {
		this(clause, description, null, status, actual, limit, unit, note);
	}

}
