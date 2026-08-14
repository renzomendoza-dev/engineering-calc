package com.renzoproject.calc_api.electrical.wiresizing;

import com.renzoproject.calc.core.electrical.wiresizing.WireSizingCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code VoltageDropService} / {@code ConduitFillService}: {@link WireSizingCalculator} is
 * plainly instantiated rather than a Spring bean, since it's a stateless POJO from calc-core.
 */
@Service
public class WireSizingService {

	private final WireSizingCalculator calculator = new WireSizingCalculator();

	public WireSizingResponse calculate(WireSizingRequest request) {
		var input = WireSizingMapper.toInput(request);
		var result = calculator.calculate(input);
		return WireSizingMapper.toResponse(result);
	}

}
