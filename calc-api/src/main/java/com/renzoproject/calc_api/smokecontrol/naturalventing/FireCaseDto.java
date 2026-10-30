package com.renzoproject.calc_api.smokecontrol.naturalventing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * One design fire in the request, mirroring calc-core's {@code FireCase}. {@code kind} is
 * {@code STEADY} or {@code GROWING}. Units: kW, s, m, m2, kW/m2.
 */
public record FireCaseDto(
		@NotBlank String name,
		@NotBlank String zoneName,
		@NotBlank String kind,
		Double steadyHeatReleaseRate,
		Double growthTime,
		Double detectionTime,
		Double designInterval,
		Double maxHeatReleaseRate,
		Double fireDiameter,
		Double fireArea,
		Double unitHeatReleaseRate,
		Double largestFireDimension,
		Double fireBaseHeight,
		@NotNull Double smokeLayerHeight) {

}
