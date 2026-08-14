package com.renzoproject.calc_api.mechanical.pipe;

import com.renzoproject.calc.core.mechanical.pipe.JsonPipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code VoltageDropService}/{@code WireSizingService}: {@link PipeVelocityCalculator} and its
 * {@link JsonPipeDimensionResolver} dependency are plainly instantiated rather than Spring
 * beans, since both are stateless POJOs from calc-core (which has no Spring dependency by
 * design).
 */
@Service
public class PipeVelocityService {

	private final PipeVelocityCalculator calculator = new PipeVelocityCalculator(new JsonPipeDimensionResolver());

	public PipeVelocityResponse calculate(PipeVelocityRequest request) {
		var input = PipeVelocityMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return PipeVelocityMapper.toResponse(result);
	}

}
