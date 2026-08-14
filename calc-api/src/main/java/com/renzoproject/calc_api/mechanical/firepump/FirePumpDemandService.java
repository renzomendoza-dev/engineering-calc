package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpDemandCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code PipeVelocityService}: {@link FirePumpDemandCalculator} is plainly instantiated rather
 * than a Spring bean, since it's a stateless POJO from calc-core.
 */
@Service
public class FirePumpDemandService {

	private final FirePumpDemandCalculator calculator = new FirePumpDemandCalculator();

	public FirePumpDemandResponse calculate(FirePumpDemandRequest request) {
		var input = FirePumpDemandMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return FirePumpDemandMapper.toResponse(result);
	}

}
