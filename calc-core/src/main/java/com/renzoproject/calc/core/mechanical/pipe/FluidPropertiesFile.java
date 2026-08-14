package com.renzoproject.calc.core.mechanical.pipe;

import java.util.List;

/**
 * Top-level shape of one {@code reference/fluids/{fluid-key}.json} file. Package-private —
 * internal to {@link JsonFluidPropertiesResolver}'s JSON parsing.
 */
record FluidPropertiesFile(
		String fluid,
		String fluidName,
		String source,
		String confidence,
		String densityUnit,
		String viscosityUnit,
		List<FluidPropertyRow> properties) {

}
