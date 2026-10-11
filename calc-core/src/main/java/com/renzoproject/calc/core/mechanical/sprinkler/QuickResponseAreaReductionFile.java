package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * Top-level shape of {@code reference/sprinkler/quick-response-area-reduction.json}. Package-private
 * -- internal to {@link JsonQuickResponseAreaReductionResolver}'s JSON parsing.
 */
record QuickResponseAreaReductionFile(
		String standard,
		String edition,
		String confidence,
		String note,
		String conditionsConfidence,
		String conditionsNote,
		double lowCeilingFt,
		double highCeilingFt,
		double reductionBelowLowCeilingPercent,
		double reductionAboveHighCeilingPercent,
		double slopePercentPerFt,
		double interceptPercent) {

}
