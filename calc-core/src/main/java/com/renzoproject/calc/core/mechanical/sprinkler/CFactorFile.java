package com.renzoproject.calc.core.mechanical.sprinkler;

import java.util.List;

/**
 * Top-level shape of {@code reference/sprinkler/hazen-williams-c.json}. Package-private --
 * internal to {@link JsonCFactorResolver}'s JSON parsing.
 */
record CFactorFile(
		String standard,
		String edition,
		String confidence,
		String note,
		String normativeTableNote,
		String annexNote,
		List<CFactorRow> cFactors) {

}
