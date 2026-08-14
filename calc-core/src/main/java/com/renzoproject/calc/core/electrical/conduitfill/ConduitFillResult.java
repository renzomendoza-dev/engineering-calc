package com.renzoproject.calc.core.electrical.conduitfill;

/**
 * @param recommendedTradeSizeMm               smallest conduit trade size (mm) that fits the
 *                                              conductors, or {@code null} if no size in the
 *                                              table for the given conduit type fits — a
 *                                              valid, expected outcome, not an error
 * @param totalConductorAreaMm2                sum of each conductor's approximate area times
 *                                              its quantity
 * @param totalConductorCount                  sum of all conductor quantities
 * @param allowedFillPercent                   the PEC Table 10.1.1.1 percentage that applies
 *                                              for this conductor count
 * @param actualFillPercentAtRecommendedSize   fill percentage actually achieved at the
 *                                              recommended size, or {@code null} matching
 *                                              {@code recommendedTradeSizeMm}
 * @param requiresMultipleConduits             {@code true} when no size fits, i.e. even the
 *                                              largest available size for this conduit type
 *                                              doesn't accommodate the conductors
 * @param practicalFillAdvisory                field-experience pull-ease note, deliberately
 *                                              separate from PEC compliance — see
 *                                              {@link PracticalFillAdvisory}. {@code null}
 *                                              when {@code requiresMultipleConduits} is
 *                                              {@code true} (no valid trade size to evaluate
 *                                              pull-ease against)
 */
public record ConduitFillResult(
		String recommendedTradeSizeMm,
		double totalConductorAreaMm2,
		int totalConductorCount,
		double allowedFillPercent,
		Double actualFillPercentAtRecommendedSize,
		boolean requiresMultipleConduits,
		PracticalFillAdvisory practicalFillAdvisory) {

}
