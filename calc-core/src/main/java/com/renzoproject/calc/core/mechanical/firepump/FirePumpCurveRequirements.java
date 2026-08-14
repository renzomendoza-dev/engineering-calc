package com.renzoproject.calc.core.mechanical.firepump;

/**
 * NFPA 20's pump curve shape rule, loaded from {@code curve-requirements.json} rather than
 * hardcoded — see {@link FirePumpCurveRequirementsLoader}.
 */
public record FirePumpCurveRequirements(
		double churnMaxPercentOfRated,
		double overloadFlowPercentOfRated,
		double overloadMinPressurePercentOfRated) {

}
