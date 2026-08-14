package com.renzoproject.calc.core.mechanical.pipe;

import java.util.List;

/**
 * One published pipe material and its full schedule/size breakdown, for populating a
 * frontend material/schedule/nominal-size dropdown chain.
 *
 * @param confidence {@code "verified"} or {@code "placeholder"} — see
 *                   {@code reference/pipes/README.md}. Frontends should surface a caveat
 *                   near the dropdown for {@code "placeholder"} materials rather than
 *                   hiding it.
 */
public record PipeMaterialReference(
		String material,
		String materialName,
		String confidence,
		List<PipeScheduleReference> schedules) {

}
