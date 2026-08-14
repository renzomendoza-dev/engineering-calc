package com.renzoproject.calc.core.mechanical.pipe;

/**
 * One row of published pipe dimension data, shaped per {@code reference/pipes/README.md}.
 * Package-private — internal to {@link JsonPipeDimensionResolver}'s JSON parsing.
 */
record PipeSizeRow(
		String nominalSize,
		String nominalLabel,
		int dn,
		double outsideDiameterMm,
		double wallThicknessMm,
		double internalDiameterMm) {

}
