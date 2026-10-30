package com.renzoproject.calc.core.smokecontrol.naturalventing;

/**
 * One line of the compliance table: a clause, what was checked, the figures compared and the
 * outcome.
 *
 * @param clause      NFPA 204 (2021) clause or annex reference, e.g. {@code "5.4.1.1"}
 * @param description what is checked, in words
 * @param basis       whether the clause is a requirement of the standard
 * @param status      outcome
 * @param actual      the design value; nullable when {@link CheckStatus#OPEN}
 * @param limit       the value it is compared with; nullable when there is none
 * @param unit        unit of {@code actual} and {@code limit}; nullable
 * @param note        extra explanation; nullable
 */
public record VentingCheck(
		String clause,
		String description,
		CheckBasis basis,
		CheckStatus status,
		Double actual,
		Double limit,
		String unit,
		String note) {

}
