package com.renzoproject.calc_api.smokecontrol.naturalventing;

import com.renzoproject.calc.core.smokecontrol.naturalventing.NaturalVentingCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as the other smoke
 * control services. The calculator needs no reference data, so it is plainly constructed.
 */
@Service
public class NaturalVentingService {

	private final NaturalVentingCalculator calculator = new NaturalVentingCalculator();

	public NaturalVentingResponse calculate(NaturalVentingRequest request) {
		var result = calculator.calculate(NaturalVentingMapper.toCoreInput(request));
		return NaturalVentingMapper.toResponse(result);
	}

}
