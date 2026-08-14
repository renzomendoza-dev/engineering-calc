package com.renzoproject.calc_api.electrical.conduitfill;

import com.renzoproject.calc.core.electrical.conduitfill.ConduitFillCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code VoltageDropService}: {@link ConduitFillCalculator} is plainly instantiated rather
 * than a Spring bean, since it's a stateless POJO from calc-core.
 */
@Service
public class ConduitFillService {

	private final ConduitFillCalculator calculator = new ConduitFillCalculator();

	public ConduitFillResponse calculate(ConduitFillRequest request) {
		var input = ConduitFillMapper.toInput(request);
		var result = calculator.calculate(input);
		return ConduitFillMapper.toResponse(result);
	}

}
