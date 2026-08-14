package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc.core.electrical.motorflc.MotorConductorSizingCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@link MotorFlcService}.
 */
@Service
public class MotorConductorSizingService {

	private final MotorConductorSizingCalculator calculator = new MotorConductorSizingCalculator();

	public MotorConductorSizingResponse calculate(MotorConductorSizingRequest request) {
		var input = MotorConductorSizingMapper.toInput(request);
		var result = calculator.calculate(input);
		return MotorConductorSizingMapper.toResponse(result);
	}

}
