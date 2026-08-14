package com.renzoproject.calc.core.mechanical.pipe;

import java.util.List;

/**
 * Top-level shape of one {@code reference/pipes/{material}.json} file. Package-private —
 * internal to {@link JsonPipeDimensionResolver}'s JSON parsing.
 */
record PipeDimensionFile(
		String material,
		String materialName,
		String standard,
		String source,
		String confidence,
		List<PipeScheduleGroup> schedules) {

}
