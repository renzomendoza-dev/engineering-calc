package com.renzoproject.calc_api.mechanical.pipe;

import com.renzoproject.calc.core.mechanical.pipe.JsonFluidPropertiesResolver;
import com.renzoproject.calc.core.mechanical.pipe.JsonPipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code PipeVelocityService}. {@code JsonPipeDimensionResolver} implements both
 * {@code PipeDimensionResolver} and {@code PipeRoughnessResolver}, so a single instance is
 * shared for both constructor parameters rather than eagerly loading the same reference JSON
 * files twice.
 */
@Service
public class PipePressureLossService {

	private final JsonPipeDimensionResolver dimensionResolver = new JsonPipeDimensionResolver();
	private final PipePressureLossCalculator calculator =
			new PipePressureLossCalculator(dimensionResolver, new JsonFluidPropertiesResolver(), dimensionResolver);

	public PipePressureLossResponse calculate(PipePressureLossRequest request) {
		var input = PipePressureLossMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return PipePressureLossMapper.toResponse(result);
	}

}
