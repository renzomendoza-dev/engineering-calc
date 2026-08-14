package com.renzoproject.calc.core.mechanical.pipe;

import java.util.List;

/** Package-private — internal to {@link JsonPipeDimensionResolver}'s JSON parsing. */
record PipeScheduleGroup(String schedule, List<PipeSizeRow> sizes) {

}
