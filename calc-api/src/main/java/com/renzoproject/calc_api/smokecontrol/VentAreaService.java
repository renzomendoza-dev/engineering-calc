package com.renzoproject.calc_api.smokecontrol;

import com.renzoproject.calc.core.common.AirPropertiesResolver;
import com.renzoproject.calc.core.smokecontrol.SmokeControlDefaultsResolver;
import com.renzoproject.calc.core.smokecontrol.VentAreaCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code SmokeProductionService}: shared resolvers injected, calculator plainly constructed.
 *
 * <p>Deliberately has no dependency on {@code SmokeProductionService}/
 * {@code TSquaredSmokeProductionService} -- preserves the decoupling decided at the core layer
 * ({@link VentAreaCalculator} takes plain values, not another calculator's result).
 */
@Service
public class VentAreaService {

	private final VentAreaCalculator calculator;

	public VentAreaService(AirPropertiesResolver airPropertiesResolver, SmokeControlDefaultsResolver defaultsResolver) {
		this.calculator = new VentAreaCalculator(airPropertiesResolver, defaultsResolver);
	}

	public VentAreaResponse calculate(VentAreaRequest request) {
		var input = VentAreaMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return VentAreaMapper.toResponse(result);
	}

}
