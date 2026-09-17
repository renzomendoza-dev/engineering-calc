package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.common.AirPropertiesResolver;
import com.renzoproject.calc.core.smokecontrol.SmokeControlDefaultsResolver;
import com.renzoproject.calc.core.smokecontrol.TSquaredSmokeProductionCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code SmokeProductionService}: shared resolvers injected, calculator plainly constructed.
 */
@Service
public class TSquaredSmokeProductionService {

	private final TSquaredSmokeProductionCalculator calculator;

	public TSquaredSmokeProductionService(AirPropertiesResolver airPropertiesResolver, SmokeControlDefaultsResolver defaultsResolver) {
		this.calculator = new TSquaredSmokeProductionCalculator(airPropertiesResolver, defaultsResolver);
	}

	public TSquaredSmokeProductionResponse calculate(TSquaredSmokeProductionRequest request) {
		var input = TSquaredSmokeProductionMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return TSquaredSmokeProductionMapper.toResponse(result);
	}

}
