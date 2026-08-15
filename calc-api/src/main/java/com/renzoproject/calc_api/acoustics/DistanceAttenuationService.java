package com.renzoproject.calc_api.acoustics;

import com.renzoproject.calc.core.acoustics.DistanceAttenuationCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as every other service
 * in this codebase: {@link DistanceAttenuationCalculator} is plainly instantiated rather than a
 * Spring bean, since it's a stateless, dependency-free POJO from calc-core (which has no Spring
 * dependency by design).
 */
@Service
public class DistanceAttenuationService {

	private final DistanceAttenuationCalculator calculator = new DistanceAttenuationCalculator();

	public DistanceAttenuationResponse calculate(DistanceAttenuationRequest request) {
		var input = DistanceAttenuationMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return DistanceAttenuationMapper.toResponse(result);
	}

}
