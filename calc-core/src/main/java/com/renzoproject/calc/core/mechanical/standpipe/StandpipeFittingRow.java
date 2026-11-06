package com.renzoproject.calc.core.mechanical.standpipe;

import java.util.Map;

/**
 * One fitting type of the equivalent length chart, keyed by nominal size.
 *
 * @param fittingType key used by {@code StandpipeFitting.fittingType()}
 * @param confidence  {@code verified} or {@code verify}
 */
public record StandpipeFittingRow(
		String fittingType,
		String label,
		String confidence,
		Map<String, Double> equivalentLengthFtByNominalSize) {

	public StandpipeFittingRow {
		equivalentLengthFtByNominalSize = equivalentLengthFtByNominalSize == null
				? Map.of()
				: Map.copyOf(equivalentLengthFtByNominalSize);
	}

}
