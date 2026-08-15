package com.renzoproject.calc_api.mechanical.pump;

import com.renzoproject.calc.core.mechanical.pump.JsonPumpMotorSizeResolver;
import com.renzoproject.calc.core.mechanical.pump.PumpPowerCalculator;
import org.springframework.stereotype.Service;

@Service
public class PumpPowerService {

	private final PumpPowerCalculator calculator = new PumpPowerCalculator(new JsonPumpMotorSizeResolver());

	public PumpPowerResponse calculate(PumpPowerRequest request) {
		var input = PumpPowerMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return PumpPowerMapper.toResponse(result);
	}

}
