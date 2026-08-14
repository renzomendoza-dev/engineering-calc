package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpCurveValidationCalculator;
import com.renzoproject.calc.core.mechanical.firepump.JsonFirePumpCurveRequirementsLoader;
import org.springframework.stereotype.Service;

@Service
public class FirePumpCurveValidationService {

	private final FirePumpCurveValidationCalculator calculator =
			new FirePumpCurveValidationCalculator(new JsonFirePumpCurveRequirementsLoader());

	public FirePumpCurveValidationResponse validate(FirePumpCurveValidationRequest request) {
		var input = FirePumpCurveValidationMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return FirePumpCurveValidationMapper.toResponse(result);
	}

}
