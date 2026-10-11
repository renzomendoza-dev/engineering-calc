package com.renzoproject.calc.core.mechanical.sprinkler;

import java.util.List;

/**
 * Top-level shape of {@code reference/sprinkler/hazard-presets.json}. Package-private -- internal
 * to {@link JsonHazardPresetResolver}'s JSON parsing.
 */
record HazardPresetFile(
		String standard,
		String edition,
		String confidence,
		String note,
		List<HazardPresetRow> presets) {

}
