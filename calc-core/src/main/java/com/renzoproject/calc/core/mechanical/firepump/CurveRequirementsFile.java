package com.renzoproject.calc.core.mechanical.firepump;

/**
 * Shape of {@code reference/firepump/curve-requirements.json}. Package-private — internal to
 * {@link JsonFirePumpCurveRequirementsLoader}'s JSON parsing.
 */
record CurveRequirementsFile(
		String standard,
		String confidence,
		double churnMaxPercentOfRated,
		double overloadFlowPercentOfRated,
		double overloadMinPressurePercentOfRated) {

}
