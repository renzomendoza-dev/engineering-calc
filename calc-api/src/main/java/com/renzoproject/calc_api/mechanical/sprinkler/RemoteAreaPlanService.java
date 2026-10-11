package com.renzoproject.calc_api.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.sprinkler.QuickResponseAreaReductionCalculator;
import com.renzoproject.calc.core.mechanical.sprinkler.QuickResponseAreaReductionResolver;
import com.renzoproject.calc.core.mechanical.sprinkler.RemoteAreaPlanCalculator;
import com.renzoproject.calc.core.mechanical.sprinkler.RemoteAreaRulesResolver;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same shape as
 * {@link SprinklerHydraulicService}. The rules resolver is the same shared instance the hydraulic
 * calculation uses, which is what keeps the plan's first-sprinkler pressure and the calculation's
 * starting pressure on one minimum.
 *
 * <p>Stateless and side-effect free, so a form can call it on each debounced input change.
 */
@Service
public class RemoteAreaPlanService {

	private final RemoteAreaPlanCalculator calculator;

	public RemoteAreaPlanService(
			RemoteAreaRulesResolver rulesResolver, QuickResponseAreaReductionResolver reductionResolver) {
		this.calculator = new RemoteAreaPlanCalculator(
				rulesResolver, new QuickResponseAreaReductionCalculator(reductionResolver));
	}

	public RemoteAreaPlanResponse plan(RemoteAreaPlanRequest request) {
		var input = RemoteAreaPlanMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return RemoteAreaPlanMapper.toResponse(result);
	}

}
