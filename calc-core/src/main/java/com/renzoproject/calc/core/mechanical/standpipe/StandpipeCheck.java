package com.renzoproject.calc.core.mechanical.standpipe;

/**
 * One line of the compliance table.
 *
 * @param clause      NFPA 14 (2019 edition) clause, e.g. {@code "7.8.1"}; must be re-checked against the 2024 edition
 * @param description what is checked
 * @param status      outcome
 * @param actual      the design value; nullable
 * @param limit       the value it is compared with; nullable
 * @param unit        unit of {@code actual} and {@code limit}; nullable
 * @param note        extra explanation; nullable
 */
public record StandpipeCheck(
		String clause,
		String description,
		CheckStatus status,
		Double actual,
		Double limit,
		String unit,
		String note) {

}
