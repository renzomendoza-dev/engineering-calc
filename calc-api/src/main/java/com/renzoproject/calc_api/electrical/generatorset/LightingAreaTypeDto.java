package com.renzoproject.calc_api.electrical.generatorset;

/**
 * An area type that takes its own unit load inside any occupancy but dwellings. <b>Units:</b> VA/m2.
 *
 * @param key              what a lighting area's {@code areaType} carries
 * @param label            the area type as printed
 * @param voltAmperesPerM2 its unit load; blank for the occupancy's own
 */
public record LightingAreaTypeDto(String key, String label, Double voltAmperesPerM2) {

}
