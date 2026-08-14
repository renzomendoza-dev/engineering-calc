package com.renzoproject.calc_api.mechanical.firepump;

public record FirePumpCurveValidationResponse(
		boolean meetsCapacityDemand,
		Double churnPressurePsi,
		boolean churnCompliant,
		Double overloadPressurePsi,
		boolean overloadCompliant,
		boolean overallCompliant) {

}
