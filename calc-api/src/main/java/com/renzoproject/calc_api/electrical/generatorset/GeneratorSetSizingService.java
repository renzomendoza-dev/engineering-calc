package com.renzoproject.calc_api.electrical.generatorset;

import com.renzoproject.calc.core.electrical.generatorset.GeneratorSetSizingCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code MotorConductorSizingService}.
 */
@Service
public class GeneratorSetSizingService {

	private final GeneratorSetSizingCalculator calculator = new GeneratorSetSizingCalculator();

	public GeneratorSetSizingResponse calculate(GeneratorSetSizingRequest request) {
		var input = GeneratorSetSizingMapper.toInput(request);
		var result = calculator.calculate(input);
		return GeneratorSetSizingMapper.toResponse(result);
	}

}
