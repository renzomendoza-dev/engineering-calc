package com.renzoproject.calc_api.acoustics;

import com.renzoproject.calc.core.acoustics.FireAlarmAudibilityCalculator;
import com.renzoproject.calc.core.acoustics.JsonAudibilityThresholdResolver;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code FirePumpPowerService}: the resolver-backed {@link FireAlarmAudibilityCalculator} is
 * plainly instantiated (not a Spring bean) with a {@link JsonAudibilityThresholdResolver},
 * since both are stateless, dependency-free POJOs from calc-core.
 */
@Service
public class FireAlarmAudibilityService {

	private final FireAlarmAudibilityCalculator calculator = new FireAlarmAudibilityCalculator(new JsonAudibilityThresholdResolver());

	public FireAlarmAudibilityResponse calculate(FireAlarmAudibilityRequest request) {
		var input = FireAlarmAudibilityMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return FireAlarmAudibilityMapper.toResponse(result);
	}

}
