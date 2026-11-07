package com.renzoproject.calc_api.smokecontrol.naturalventing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * One curtained area in the request, mirroring calc-core's {@code VentZone}. Units: m, m2, K,
 * kW/m2. Structural checks only; the ranges are enforced by calc-core. {@code eaveHeight} (optional) is
 * the eave of a gable roof whose ridge is {@code ceilingHeight}, for the time-stepping check; left out,
 * the ceiling is flat. {@code ventCentreHeight} may be above the ceiling (a raised roof monitor).
 */
public record VentZoneDto(
		@NotBlank String name,
		@NotNull Double ceilingHeight,
		Double ventCentreHeight,
		Double curtainDepth,
		Double zoneLength,
		Double zoneWidth,
		@NotNull Double ventDischargeCoefficient,
		Integer ventCount,
		Double ventTotalArea,
		Double ventUnitLength,
		Double ventUnitWidth,
		Double ventSpacing,
		Double ventToWallDistance,
		Double inletDischargeCoefficient,
		Double inletAreaRatio,
		Double inletNetArea,
		Double maxLayerTemperatureRise,
		Double maxFloorRadiantFlux,
		Double aisleWidth,
		Double eaveHeight) {

}
