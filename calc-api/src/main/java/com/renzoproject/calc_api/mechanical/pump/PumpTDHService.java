package com.renzoproject.calc_api.mechanical.pump;

import com.renzoproject.calc.core.mechanical.pipe.JsonFluidPropertiesResolver;
import com.renzoproject.calc.core.mechanical.pipe.JsonPipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossCalculator;
import com.renzoproject.calc.core.mechanical.pump.PumpTDHCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core. {@link PumpTDHCalculator} itself
 * internally calls {@link PipePressureLossCalculator} — that composition happened in calc-core;
 * this service only wires the plain-Java dependency graph (calc-core has no Spring DI of its
 * own), same pattern as {@code PipePressureLossService}.
 */
@Service
public class PumpTDHService {

	private final JsonPipeDimensionResolver dimensionResolver = new JsonPipeDimensionResolver();
	private final JsonFluidPropertiesResolver fluidPropertiesResolver = new JsonFluidPropertiesResolver();
	private final PipePressureLossCalculator pressureLossCalculator =
			new PipePressureLossCalculator(dimensionResolver, fluidPropertiesResolver, dimensionResolver);
	private final PumpTDHCalculator calculator = new PumpTDHCalculator(pressureLossCalculator, fluidPropertiesResolver);

	public PumpTDHResponse calculate(PumpTDHRequest request) {
		var input = PumpTDHMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return PumpTDHMapper.toResponse(result);
	}

}
