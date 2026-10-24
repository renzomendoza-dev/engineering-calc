package com.renzoproject.calc_api.electrical.generatorset;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * A group of receptacle outlets, mirroring calc-core's {@code GeneratorSetSizingInput.ReceptacleGroup}.
 *
 * @param label                   name shown on the load schedule
 * @param outlets                 number of outlets, each a single or multiple receptacle on one yoke
 * @param bankOrOfficeFloorAreaM2 floor area when the outlets are in a bank or office building, where the load
 *                                is the larger of 180 VA per outlet or 11 VA/m2 (2.20.2.5(K)); blank otherwise
 * @param continuous              whether the load runs three hours or more
 */
public record ReceptacleGroupDto(
		String label,
		@Min(1) int outlets,
		@Positive Double bankOrOfficeFloorAreaM2,
		boolean continuous) {

}
