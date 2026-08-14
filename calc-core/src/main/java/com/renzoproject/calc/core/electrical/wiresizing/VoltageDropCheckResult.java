package com.renzoproject.calc.core.electrical.wiresizing;

/**
 * @param sizeLabelChecked        the ampacity-based recommended size that voltage drop was
 *                                 evaluated against
 * @param voltageDropPercent      voltage drop percent at {@code sizeLabelChecked}
 * @param exceedsRecommendedLimit whether {@code sizeLabelChecked} exceeds
 *                                 {@code VoltageDropCalculator.RECOMMENDED_MAX_PERCENT}
 * @param upsizedRecommendation   a larger size that satisfies both ampacity and voltage drop,
 *                                 populated only when {@code exceedsRecommendedLimit} is
 *                                 {@code true} and such a size was found; {@code null} if
 *                                 voltage drop passed, or if no larger size in the table fixes
 *                                 it (a valid outcome, not an error — see
 *                                 {@link WireSizingCalculator})
 */
public record VoltageDropCheckResult(
		String sizeLabelChecked,
		double voltageDropPercent,
		boolean exceedsRecommendedLimit,
		String upsizedRecommendation) {

}
