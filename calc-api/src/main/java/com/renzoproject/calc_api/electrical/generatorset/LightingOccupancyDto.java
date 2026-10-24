package com.renzoproject.calc_api.electrical.generatorset;

/**
 * One occupancy of PEC Table 2.20.2.3. <b>Units:</b> VA/m2.
 *
 * @param key                           what a lighting area's {@code occupancy} carries
 * @param label                         the occupancy as printed
 * @param voltAmperesPerM2              the table's minimum unit load
 * @param fullDemandWhenAllLightingInUse whether the demand factor can be skipped where the whole lighting is
 *                                      used at once (hospitals, hotels)
 * @param note                          anything to know about receptacles or demand for this occupancy; may be blank
 */
public record LightingOccupancyDto(
		String key,
		String label,
		double voltAmperesPerM2,
		boolean fullDemandWhenAllLightingInUse,
		String note) {

}
