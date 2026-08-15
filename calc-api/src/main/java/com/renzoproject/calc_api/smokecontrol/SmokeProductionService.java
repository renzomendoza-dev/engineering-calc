package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.common.JsonAirPropertiesResolver;
import com.renzoproject.calc.core.smokecontrol.JsonSmokeControlDefaultsResolver;
import com.renzoproject.calc.core.smokecontrol.SmokeProductionCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code PipeVelocityService}: {@link SmokeProductionCalculator} and its two resolver
 * dependencies are plainly instantiated rather than Spring beans, since all three are stateless,
 * dependency-free POJOs from calc-core (which has no Spring dependency by design).
 */
@Service
public class SmokeProductionService {

	private final SmokeProductionCalculator calculator =
			new SmokeProductionCalculator(new JsonAirPropertiesResolver(), new JsonSmokeControlDefaultsResolver());

	public SmokeProductionResponse calculate(SmokeProductionRequest request) {
		var input = SmokeProductionMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return SmokeProductionMapper.toResponse(result);
	}

}
