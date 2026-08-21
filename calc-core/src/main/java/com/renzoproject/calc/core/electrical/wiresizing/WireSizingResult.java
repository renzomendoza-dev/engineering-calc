package com.renzoproject.calc.core.electrical.wiresizing;

/**
 * @param recommendedSizeLabel      smallest conductor size whose derated ampacity satisfies
 *                                   {@code requiredAmpacityPerSetAmps} — this is the size of
 *                                   ONE conductor; the caller needs {@code numberOfParallelSets}
 *                                   of them, run in parallel, to carry the full load
 * @param baseAmpacityAmps           published ampacity of {@code recommendedSizeLabel} at the
 *                                   insulation's own temperature rating, before derating
 * @param tempCorrectionFactor       ambient temperature correction factor applied
 * @param adjustmentFactor           conductor-count adjustment factor applied (already
 *                                   divided by 100, e.g. {@code 0.8} for a table value of 80)
 * @param deratedAmpacityAmps        {@code baseAmpacityAmps * tempCorrectionFactor *
 *                                   adjustmentFactor} — one conductor's derated ampacity
 * @param requiredAmpacityAmps       {@code loadCurrentAmps}, times 1.25 if continuous — the
 *                                   TOTAL ampacity the full set of parallel conductors must
 *                                   carry together
 * @param numberOfParallelSets       echoed from the input; {@code 1} for the ordinary
 *                                   single-conductor case
 * @param requiredAmpacityPerSetAmps {@code requiredAmpacityAmps / numberOfParallelSets} — what
 *                                   each individual conductor must satisfy, and what
 *                                   {@code recommendedSizeLabel} was actually sized against
 * @param meetsTerminationRating     whether {@code recommendedSizeLabel}'s derated ampacity at
 *                                   the termination's own temperature rating (a separate
 *                                   lookup from the insulation's temp rating) still satisfies
 *                                   {@code requiredAmpacityPerSetAmps}
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
		int numberOfParallelSets,
		double requiredAmpacityPerSetAmps,
		boolean meetsTerminationRating,
		VoltageDropCheckResult voltageDropCheckResult) {

}
