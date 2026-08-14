package com.renzoproject.calc_api.electrical.motorflc;

import com.renzoproject.calc.core.electrical.motorflc.LockedRotorCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@link MotorFlcService}.
 */
@Service
public class LockedRotorService {

	private final LockedRotorCalculator calculator = new LockedRotorCalculator();

	public LockedRotorResponse calculate(LockedRotorRequest request) {
		var input = LockedRotorMapper.toInput(request);
		var result = calculator.calculate(input);
		return LockedRotorMapper.toResponse(result);
	}

}
