package com.renzoproject.calc_api.mechanical.tank;

import com.renzoproject.calc.core.mechanical.tank.PressureTankCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code ExpansionTankService}. {@link PressureTankCalculator} has no resolver dependencies, so
 * it's plainly instantiated with a no-arg constructor.
 *
 * <p>No exception handling here -- {@code CalculationException} (non-positive flow rate/run time,
 * high pressure &lt;= low pressure) propagates straight through to the existing global handler,
 * same single-exception-type discipline as every other service.
 */
@Service
public class PressureTankService {

	private final PressureTankCalculator calculator = new PressureTankCalculator();

	public PressureTankResponse calculate(PressureTankRequest request) {
		var input = PressureTankMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return PressureTankMapper.toResponse(result);
	}

}
