package com.renzoproject.calc_api.smokecontrol.naturalventing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * One design fire in the request, mirroring calc-core's {@code FireCase}. {@code kind} is
 * {@code STEADY} or {@code GROWING}. Units: kW, s, m, m2, kW/m2. For a growing fire give the
 * {@code detectionTime}, or the {@code detectorDistance} (m) it is to be worked out from by
 * Equation 9.2.5.4.3, with an optional {@code detectorTemperatureRise} (degrees C, default 20).
 */
public record FireCaseDto(
		@NotBlank String name,
		@NotBlank String zoneName,
		@NotBlank String kind,
		Double steadyHeatReleaseRate,
		Double growthTime,
		Double detectionTime,
		Double detectorDistance,
		Double detectorTemperatureRise,
		Double designInterval,
		Double maxHeatReleaseRate,
		Double fireDiameter,
		Double fireArea,
		Double unitHeatReleaseRate,
		Double largestFireDimension,
		Double fireBaseHeight,
		@NotNull Double smokeLayerHeight) {

}
