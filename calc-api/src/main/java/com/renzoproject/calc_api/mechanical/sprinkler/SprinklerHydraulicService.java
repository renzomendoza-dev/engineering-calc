package com.renzoproject.calc_api.mechanical.sprinkler;

import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeVelocityCalculator;
import com.renzoproject.calc.core.mechanical.sprinkler.FittingEquivalentLengthResolver;
import com.renzoproject.calc.core.mechanical.sprinkler.SprinklerHydraulicCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core. Resolvers are shared singletons injected
 * from {@code ResolverConfig} (the pipe dimension resolver is the same instance the pipe, pump and
 * reference endpoints use); the calculators are plainly constructed, since they hold no state
 * beyond the resolvers.
 *
 * <p>Stateless and side-effect free: every call re-runs the calculation from the request alone, so
 * a frontend can call it on each debounced input change.
 */
@Service
public class SprinklerHydraulicService {

	private final SprinklerHydraulicCalculator calculator;

	public SprinklerHydraulicService(
			PipeDimensionResolver dimensionResolver,
			FittingEquivalentLengthResolver fittingResolver) {
		this.calculator = new SprinklerHydraulicCalculator(
				dimensionResolver,
				fittingResolver,
				new PipeVelocityCalculator(dimensionResolver));
	}

	public SprinklerHydraulicResponse calculate(SprinklerHydraulicRequest request) {
		var input = SprinklerHydraulicMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return SprinklerHydraulicMapper.toResponse(result);
	}

}
