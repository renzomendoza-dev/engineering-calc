package com.renzoproject.calc.core.mechanical.sprinkler;

import java.util.List;

/**
 * Top-level shape of {@code reference/sprinkler/fitting-equivalent-lengths.json}.
 * Package-private -- internal to {@link JsonFittingEquivalentLengthResolver}'s JSON parsing.
 */
record FittingEquivalentLengthFile(
		String standard,
		String edition,
		double cFactorBasis,
		String confidence,
		String note,
		String cFactorBasisNote,
		String transcriptionNote,
		String fittingRowUnderReview,
		List<FittingEquivalentLengthRow> fittings) {

}
