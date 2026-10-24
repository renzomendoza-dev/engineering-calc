package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * One area of general lighting, mirroring calc-core's {@code GeneratorSetSizingInput.LightingArea}.
 *
 * @param label                    name shown on the load schedule; blank uses the occupancy's name
 * @param occupancy                key of a Table 2.20.2.3 occupancy, from the reference endpoint
 * @param areaType                 key of the area type; {@code GENERAL} (or blank) for the occupancy's own unit load
 * @param floorAreaM2              floor area in square metres, from the outside dimensions (2.20.2.3)
 * @param unitLoadVoltAmperesPerM2 a design unit load above the table's, in VA/m2; blank to use the table's
 * @param entireLightingUsedAtOnce whether all of it is likely to be used at one time (operating rooms,
 *                                 ballrooms, dining rooms), which skips the demand factor for hospitals and hotels
 * @param continuous               whether the load runs three hours or more
 */
public record LightingAreaDto(
		String label,
		@NotBlank String occupancy,
		String areaType,
		@Positive double floorAreaM2,
		@Positive Double unitLoadVoltAmperesPerM2,
		boolean entireLightingUsedAtOnce,
		boolean continuous) {

}
