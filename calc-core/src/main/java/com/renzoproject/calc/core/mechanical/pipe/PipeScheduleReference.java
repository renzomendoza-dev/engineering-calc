package com.renzoproject.calc.core.mechanical.pipe;

import java.util.List;

/** One published schedule (e.g. {@code "SCH40"}) and its sizes, for populating a frontend dropdown. */
public record PipeScheduleReference(String schedule, List<PipeSizeReference> sizes) {

}
