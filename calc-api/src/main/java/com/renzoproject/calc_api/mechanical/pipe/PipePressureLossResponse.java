package com.renzoproject.calc_api.mechanical.pipe;

public record PipePressureLossResponse(
		Double velocityMetersPerSecond,
		Double reynoldsNumber,
		FlowRegimeDto flowRegime,
		boolean transitionalRegimeWarning,
		Double frictionFactor,
		Double headLossMeters,
		Double pressureLossPascals) {

}
