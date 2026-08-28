package com.renzoproject.calc_api.electrical.powerconsumption;

import com.renzoproject.calc.core.electrical.powerconsumption.PowerConsumptionCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code WireSizingService}: {@link PowerConsumptionCalculator} is plainly instantiated rather
 * than a Spring bean, since it's a stateless, dependency-free POJO from calc-core.
 */
@Service
public class PowerConsumptionService {

	private final PowerConsumptionCalculator calculator = new PowerConsumptionCalculator();

	public PowerConsumptionResponse calculate(PowerConsumptionRequest request) {
		var input = PowerConsumptionMapper.toInput(request);
		var result = calculator.calculate(input);
		return PowerConsumptionMapper.toResponse(result);
	}

}
