package com.renzoproject.calc.core.mechanical.sprinkler;

import java.util.Map;

/**
 * One fitting type's published equivalent lengths, keyed by nominal size (the same
 * {@code nominalSize} keys as {@code reference/pipes/*.json}: {@code "3/4"}, {@code "1-1/4"},
 * {@code "2"}, ...).
 *
 * <p>{@code equivalentLengthFtByNominalSize} is empty while the published table has not been
 * entered -- {@link SprinklerHydraulicCalculator} then refuses the calculation rather than
 * treating a missing fitting as zero length.
 *
 * @param fittingType key used by {@code SegmentFitting.fittingType()}
 * @param confidence  {@code verified}, {@code verify} or {@code placeholder}
 */
public record FittingEquivalentLengthRow(
		String fittingType,
		String label,
		String confidence,
		Map<String, Double> equivalentLengthFtByNominalSize) {

	public FittingEquivalentLengthRow {
		equivalentLengthFtByNominalSize = equivalentLengthFtByNominalSize == null
				? Map.of()
				: Map.copyOf(equivalentLengthFtByNominalSize);
	}

}
