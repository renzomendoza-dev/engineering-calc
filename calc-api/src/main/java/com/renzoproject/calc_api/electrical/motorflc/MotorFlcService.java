package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc.core.electrical.motorflc.MotorFlcCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code WireSizingService}: {@link MotorFlcCalculator} is plainly instantiated rather than a
 * Spring bean, since it's a stateless POJO from calc-core.
 */
@Service
public class MotorFlcService {

	private final MotorFlcCalculator calculator = new MotorFlcCalculator();

	public MotorFlcResponse calculate(MotorFlcRequest request) {
		var input = MotorFlcMapper.toInput(request);
		var result = calculator.calculate(input);
		return MotorFlcMapper.toResponse(result);
	}

}
