package com.renzoproject.calc_api.smokecontrol.naturalventing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * HTTP request body for an NFPA 204 natural venting calculation. Mirrors calc-core's
 * {@code NaturalVentingInput}: {@code ambientTemperature} in degC; the optional properties
 * ({@code ambientDensity}, {@code specificHeat}, {@code layerEnergyFraction},
 * {@code growthConstant}) fall back to calc-core's defaults when omitted. Domain validation
 * stays in calc-core and surfaces as a 400 through {@code GlobalExceptionHandler}.
 */
public record NaturalVentingRequest(
		@NotNull Double ambientTemperature,
		Double ambientDensity,
		Double specificHeat,
		Double layerEnergyFraction,
		Double growthConstant,
		@NotEmpty List<@Valid VentZoneDto> zones,
		@NotEmpty List<@Valid FireCaseDto> fireCases) {

}
