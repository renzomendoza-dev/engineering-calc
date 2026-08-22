package com.renzoproject.calc.core.mechanical.storage;

import java.util.List;

/**
 * One row of {@code reference/storage/fire-water-duration.json}'s {@code classifications}.
 * Package-private -- internal to {@link JsonFireWaterDurationResolver}'s JSON parsing.
 *
 * @param hazardClassification matches a {@link HazardClassification} enum constant name exactly
 * @param combinedHoseGpm      total hose stream allowance, GPM -- becomes
 *                             {@link DurationRange#hoseStreamAllowanceGpm()}
 */
record ClassificationRow(
		String hazardClassification,
		List<Integer> insideHoseGpmOptions,
		double combinedHoseGpm,
		double combinedHoseLpm,
		double durationMinutesMin,
		double durationMinutesMax) {

}
