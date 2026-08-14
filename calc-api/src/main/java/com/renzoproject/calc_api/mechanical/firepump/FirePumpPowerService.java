package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpPowerCalculator;
import com.renzoproject.calc.core.mechanical.firepump.JsonFirePumpMotorSizeResolver;
import org.springframework.stereotype.Service;

@Service
public class FirePumpPowerService {

	private final FirePumpPowerCalculator calculator = new FirePumpPowerCalculator(new JsonFirePumpMotorSizeResolver());

	public FirePumpPowerResponse calculate(FirePumpPowerRequest request) {
		var input = FirePumpPowerMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return FirePumpPowerMapper.toResponse(result);
	}

}
