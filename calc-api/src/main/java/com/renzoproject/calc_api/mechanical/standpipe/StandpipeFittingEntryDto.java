package com.renzoproject.calc_api.mechanical.standpipe;

import java.util.Map;

/**
 * One fitting of the equivalent length chart, mirroring calc-core StandpipeFittingRow. Equivalent
 * lengths are in feet of Schedule 40 steel pipe at C = 120, keyed by nominal size.
 */
public record StandpipeFittingEntryDto(
		String fittingType,
		String label,
		String confidence,
		Map<String, Double> equivalentLengthFtByNominalSize) {

}
