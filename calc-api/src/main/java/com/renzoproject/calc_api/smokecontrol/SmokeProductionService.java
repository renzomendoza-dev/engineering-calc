package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.common.AirPropertiesResolver;
import com.renzoproject.calc.core.smokecontrol.SmokeControlDefaultsResolver;
import com.renzoproject.calc.core.smokecontrol.SmokeProductionCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core. Resolvers are shared singletons
 * injected from {@code ResolverConfig}; the calculator itself is plainly constructed, since it
 * holds no state beyond them.
 */
@Service
public class SmokeProductionService {

	private final SmokeProductionCalculator calculator;

	public SmokeProductionService(AirPropertiesResolver airPropertiesResolver, SmokeControlDefaultsResolver defaultsResolver) {
		this.calculator = new SmokeProductionCalculator(airPropertiesResolver, defaultsResolver);
	}

	public SmokeProductionResponse calculate(SmokeProductionRequest request) {
		var input = SmokeProductionMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return SmokeProductionMapper.toResponse(result);
	}

}
