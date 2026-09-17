package com.renzoproject.calc_api.acoustics;

import com.renzoproject.calc.core.acoustics.AudibilityThresholdResolver;
import com.renzoproject.calc.core.acoustics.FireAlarmAudibilityCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core. The threshold resolver is a shared
 * singleton injected from {@code ResolverConfig}; the calculator is plainly constructed.
 */
@Service
public class FireAlarmAudibilityService {

	private final FireAlarmAudibilityCalculator calculator;

	public FireAlarmAudibilityService(AudibilityThresholdResolver thresholdResolver) {
		this.calculator = new FireAlarmAudibilityCalculator(thresholdResolver);
	}

	public FireAlarmAudibilityResponse calculate(FireAlarmAudibilityRequest request) {
		var input = FireAlarmAudibilityMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return FireAlarmAudibilityMapper.toResponse(result);
	}

}
