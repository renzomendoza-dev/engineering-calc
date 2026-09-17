package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpMotorSizeResolver;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpPowerCalculator;
import org.springframework.stereotype.Service;

@Service
public class FirePumpPowerService {

	private final FirePumpPowerCalculator calculator;

	public FirePumpPowerService(FirePumpMotorSizeResolver motorSizeResolver) {
		this.calculator = new FirePumpPowerCalculator(motorSizeResolver);
	}

	public FirePumpPowerResponse calculate(FirePumpPowerRequest request) {
		var input = FirePumpPowerMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return FirePumpPowerMapper.toResponse(result);
	}

}
