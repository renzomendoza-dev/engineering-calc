package com.renzoproject.calc.core.mechanical.sprinkler;

/**
 * One row of {@code reference/sprinkler/hazard-presets.json}, as published. Public so calc-api can
 * expose the table for a form's hazard dropdown, same as {@code DuctRoughnessRow}.
 *
 * @param hazard                      hazard key, parsed into {@link SprinklerHazard}
 * @param densityGpmPerSqFt           minimum point of the density/area curve
 * @param designAreaSqFt              area paired with that density
 * @param hoseStreamGpm               hose stream allowance
 * @param durationMinutes             water supply duration (upper bound where NFPA gives a range)
 * @param maxCoverageSqFtPerSprinkler maximum protection area per sprinkler
 * @param confidence                  {@code verified}, {@code verify} or {@code placeholder}
 */
public record HazardPresetRow(
		String hazard,
		String label,
		double densityGpmPerSqFt,
		double designAreaSqFt,
		double hoseStreamGpm,
		int durationMinutes,
		double maxCoverageSqFtPerSprinkler,
		String confidence,
		String sourceNote) {

}
