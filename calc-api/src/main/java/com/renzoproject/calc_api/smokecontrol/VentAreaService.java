package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.common.JsonAirPropertiesResolver;
import com.renzoproject.calc.core.smokecontrol.JsonSmokeControlDefaultsResolver;
import com.renzoproject.calc.core.smokecontrol.VentAreaCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code SmokeProductionService}: {@link VentAreaCalculator} and its two resolver dependencies
 * are plainly instantiated rather than Spring beans, since all three are stateless,
 * dependency-free POJOs from calc-core.
 *
 * <p>Deliberately has no dependency on {@code SmokeProductionService}/
 * {@code TSquaredSmokeProductionService} -- preserves the decoupling decided at the core layer
 * ({@link VentAreaCalculator} takes plain values, not another calculator's result).
 */
@Service
public class VentAreaService {

	private final VentAreaCalculator calculator =
			new VentAreaCalculator(new JsonAirPropertiesResolver(), new JsonSmokeControlDefaultsResolver());

	public VentAreaResponse calculate(VentAreaRequest request) {
		var input = VentAreaMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return VentAreaMapper.toResponse(result);
	}

}
