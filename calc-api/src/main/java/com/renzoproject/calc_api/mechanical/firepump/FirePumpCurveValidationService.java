package com.renzoproject.calc_api.mechanical.firepump;

import com.renzoproject.calc.core.mechanical.firepump.FirePumpCurveRequirementsLoader;
import com.renzoproject.calc.core.mechanical.firepump.FirePumpCurveValidationCalculator;
import org.springframework.stereotype.Service;

@Service
public class FirePumpCurveValidationService {

	private final FirePumpCurveValidationCalculator calculator;

	public FirePumpCurveValidationService(FirePumpCurveRequirementsLoader requirementsLoader) {
		this.calculator = new FirePumpCurveValidationCalculator(requirementsLoader);
	}

	public FirePumpCurveValidationResponse validate(FirePumpCurveValidationRequest request) {
		var input = FirePumpCurveValidationMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return FirePumpCurveValidationMapper.toResponse(result);
	}

}
