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
 * @param maximumOvercurrentDeviceAmps the largest overcurrent device that may protect
 *                                   {@code recommendedSizeLabel} under PEC 2.40.1.4(D) Small
 *                                   Conductors, A; {@code null} for a size the clause does not
 *                                   cover (5.5 mm2 aluminum and 8.0 mm2 copper upward have no
 *                                   such cap). Applied after the corrections for ambient
 *                                   temperature and number of conductors
 * @param overcurrentDeviceAmps      the overcurrent device rating given in the input; {@code null}
 *                                   when none was
 * @param ampacitySizeLabel          the smallest size that satisfies the ampacity alone, before the
 *                                   small-conductor limit; equals {@code recommendedSizeLabel}
 *                                   unless {@code sizeRaisedForOvercurrentLimit}
 * @param sizeRaisedForOvercurrentLimit whether the overcurrent device given may not protect
 *                                   {@code ampacitySizeLabel} under PEC 2.40.1.4, so a larger size
 *                                   is recommended; {@code overcurrentLimitClause} says why
 * @param nipple                     echoed from the input: the run is a nipple of not more than
 *                                   600 mm, so {@code adjustmentFactor} is 1 whatever the number of
 *                                   conductors (PEC Chapter 10, note (4))
 * @param protectedAmpacityAmps      the ampacity the overcurrent device protects: {@code
 *                                   deratedAmpacityAmps} times {@code numberOfParallelSets}
 * @param standardRatingLimitAmps    the largest standard rating of Table 2.40.1.6(A) that may protect
 *                                   {@code protectedAmpacityAmps} under 2.40.1.4(B) and (C): equal to
 *                                   the ampacity, or the next one above it up to 800 A, or else the
 *                                   largest not above it. {@code null} when none fits. The (D) limit
 *                                   ({@code maximumOvercurrentDeviceAmps}) can be lower still
 * @param multipleReceptacleBranchCircuit echoed from the input: (B) does not apply, so the device
 *                                   may not exceed the ampacity
 * @param overcurrentLimitClause     the clause of 2.40.1.4 that did not let the given device protect
 *                                   {@code ampacitySizeLabel}: {@code "2.40.1.4(D)"}, {@code "(B)"} or
 *                                   {@code "(C)"}; {@code null} unless {@code sizeRaisedForOvercurrentLimit}
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
		VoltageDropCheckResult voltageDropCheckResult,
		Integer maximumOvercurrentDeviceAmps,
		Double overcurrentDeviceAmps,
		String ampacitySizeLabel,
		boolean sizeRaisedForOvercurrentLimit,
		boolean nipple,
		double protectedAmpacityAmps,
		Integer standardRatingLimitAmps,
		boolean multipleReceptacleBranchCircuit,
		String overcurrentLimitClause) {

}
