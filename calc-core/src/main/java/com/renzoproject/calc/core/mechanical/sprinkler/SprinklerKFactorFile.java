package com.renzoproject.calc.core.mechanical.sprinkler;

import java.util.List;

/**
 * Top-level shape of {@code reference/sprinkler/sprinkler-k-factors.json}. Package-private --
 * internal to {@link JsonSprinklerKFactorResolver}'s JSON parsing.
 */
record SprinklerKFactorFile(
		String standard,
		String edition,
		String confidence,
		String note,
		List<SprinklerKFactorRow> kFactors) {

}
