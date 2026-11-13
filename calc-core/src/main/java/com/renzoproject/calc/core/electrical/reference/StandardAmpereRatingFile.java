package com.renzoproject.calc.core.electrical.reference;

import java.util.List;

/**
 * Shape of {@code reference/electrical/standard-ampere-ratings.json}. Package-private: internal to
 * {@link StandardAmpereRatingTable}.
 */
record StandardAmpereRatingFile(
		String standard,
		String edition,
		String confidence,
		String note,
		int nextHigherRatingMaximumAmps,
		List<Integer> ratings,
		List<Integer> additionalFuseRatings) {

}
