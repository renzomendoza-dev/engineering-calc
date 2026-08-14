package com.renzoproject.calc.core.electrical.wiresizing;

/**
 * @param recommendedSizeLabel      smallest conductor size whose derated ampacity satisfies
 *                                   {@code requiredAmpacityAmps}
 * @param baseAmpacityAmps           published ampacity of {@code recommendedSizeLabel} at the
 *                                   insulation's own temperature rating, before derating
 * @param tempCorrectionFactor       ambient temperature correction factor applied
 * @param adjustmentFactor           conductor-count adjustment factor applied (already
 *                                   divided by 100, e.g. {@code 0.8} for a table value of 80)
 * @param deratedAmpacityAmps        {@code baseAmpacityAmps * tempCorrectionFactor *
 *                                   adjustmentFactor}
 * @param requiredAmpacityAmps       {@code loadCurrentAmps}, times 1.25 if continuous
 * @param meetsTerminationRating     whether {@code recommendedSizeLabel}'s derated ampacity at
 *                                   the termination's own temperature rating (a separate
 *                                   lookup from the insulation's temp rating) still satisfies
 *                                   {@code requiredAmpacityAmps}
 * @param voltageDropCheckResult     present only if a voltage drop check was requested in the
 *                                   input; {@code null} otherwise
 */
public record WireSizingResult(
		String recommendedSizeLabel,
		double baseAmpacityAmps,
		double tempCorrectionFactor,
		double adjustmentFactor,
		double deratedAmpacityAmps,
		double requiredAmpacityAmps,
		boolean meetsTerminationRating,
		VoltageDropCheckResult voltageDropCheckResult) {

}
