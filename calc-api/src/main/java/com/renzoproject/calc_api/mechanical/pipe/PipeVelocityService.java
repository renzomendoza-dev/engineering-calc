package com.renzoproject.calc_api.mechanical.pipe;

import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core. The dimension resolver is a shared
 * singleton injected from {@code ResolverConfig}; the calculator is plainly constructed.
 */
@Service
public class PipeVelocityService {

	private final PipeVelocityCalculator calculator;

	public PipeVelocityService(PipeDimensionResolver dimensionResolver) {
		this.calculator = new PipeVelocityCalculator(dimensionResolver);
	}

	public PipeVelocityResponse calculate(PipeVelocityRequest request) {
		var input = PipeVelocityMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return PipeVelocityMapper.toResponse(result);
	}

}
