package com.renzoproject.calc_api.mechanical.pipe;

import com.renzoproject.calc.core.mechanical.pipe.FluidPropertiesResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossCalculator;
import com.renzoproject.calc.core.mechanical.pipe.PipeRoughnessResolver;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core. Resolvers are shared singletons
 * injected from {@code ResolverConfig}; the calculator is plainly constructed.
 *
 * <p>{@code dimensionResolver} and {@code roughnessResolver} resolve to the same
 * {@code JsonPipeDimensionResolver} bean, which implements both interfaces -- so the pipe
 * reference JSON is parsed once for the whole application, not once per service.
 */
@Service
public class PipePressureLossService {

	private final PipePressureLossCalculator calculator;

	public PipePressureLossService(
			PipeDimensionResolver dimensionResolver,
			FluidPropertiesResolver fluidPropertiesResolver,
			PipeRoughnessResolver roughnessResolver) {
		this.calculator = new PipePressureLossCalculator(dimensionResolver, fluidPropertiesResolver, roughnessResolver);
	}

	public PipePressureLossResponse calculate(PipePressureLossRequest request) {
		var input = PipePressureLossMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return PipePressureLossMapper.toResponse(result);
	}

}
