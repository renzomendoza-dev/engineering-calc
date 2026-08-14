package com.renzoproject.calc_api.mechanical.firepump;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record FirePumpCurveValidationRequest(
		@NotNull @Positive Double candidateRatedFlowGpm,
		@NotNull @Positive Double candidateRatedPressurePsi,
		@NotEmpty List<@Valid PumpCurvePointDto> curvePoints,
		@NotNull @Positive Double demandFlowGpm,
		@NotNull @Positive Double demandPressurePsi) {

}
