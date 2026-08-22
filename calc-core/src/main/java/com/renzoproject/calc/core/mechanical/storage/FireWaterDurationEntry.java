package com.renzoproject.calc.core.mechanical.storage;

/**
 * One row of {@code reference/storage/fire-water-duration.json}'s {@code classifications},
 * keyed by {@link HazardClassification} rather than the raw JSON's string field. Public,
 * unlike the package-private {@code ClassificationRow} it's built from, so
 * {@link FireWaterDurationResolver#allEntries()} can return it for display purposes.
 */
public record FireWaterDurationEntry(
		HazardClassification hazardClassification,
		double minMinutes,
		double maxMinutes,
		double hoseStreamAllowanceGpm) {

}
