package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.common.JsonAirPropertiesResolver;
import com.renzoproject.calc.core.smokecontrol.JsonSmokeControlDefaultsResolver;
import com.renzoproject.calc.core.smokecontrol.TSquaredSmokeProductionCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code SmokeProductionService}: {@link TSquaredSmokeProductionCalculator} and its two resolver
 * dependencies are plainly instantiated rather than Spring beans, since all three are stateless,
 * dependency-free POJOs from calc-core (which has no Spring dependency by design).
 */
@Service
public class TSquaredSmokeProductionService {

	private final TSquaredSmokeProductionCalculator calculator =
			new TSquaredSmokeProductionCalculator(new JsonAirPropertiesResolver(), new JsonSmokeControlDefaultsResolver());

	public TSquaredSmokeProductionResponse calculate(TSquaredSmokeProductionRequest request) {
		var input = TSquaredSmokeProductionMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return TSquaredSmokeProductionMapper.toResponse(result);
	}

}
