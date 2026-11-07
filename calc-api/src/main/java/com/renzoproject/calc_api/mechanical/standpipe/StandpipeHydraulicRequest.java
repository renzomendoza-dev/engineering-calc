package com.renzoproject.calc_api.mechanical.standpipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * HTTP request body for a standpipe hydraulic calculation, mirroring calc-core
 * StandpipeHydraulicInput. {@code standpipeClass} is CLASS_I, CLASS_II or CLASS_III;
 * {@code sprinklerProtection} is NONE, FULL or PARTIAL; {@code sprinklerHazard} is LIGHT or
 * ORDINARY. {@code fireDepartmentConnectionInletSize} is TWO_AND_HALF_INCH (the default), FOUR_INCH or
 * FIVE_INCH. {@code singleConnectionLaterals}, {@code risersEndAtDifferentLevels} and
 * {@code manualSystem} may be left out and mean none, no and no. Domain rules stay in calc-core and
 * surface as a 400 through GlobalExceptionHandler.
 */
public record StandpipeHydraulicRequest(
		@NotBlank String standpipeClass,
		String sprinklerProtection,
		String sprinklerHazard,
		Double sprinklerDemandGpm,
		@NotNull Integer additionalStandpipes,
		@NotNull Boolean floorAreaOver80000SqFt,
		@NotNull Boolean horizontalStandpipeOnThreeConnections,
		Integer singleConnectionLaterals,
		Boolean risersEndAtDifferentLevels,
		Boolean manualSystem,
		String fireDepartmentConnectionInletSize,
		Integer fireDepartmentConnectionInlets,
		Double supplyDurationMinutes,
		Double flowAddedAtSupplyGpm,
		@NotEmpty List<@Valid StandpipeSegmentDto> segments) {

}
