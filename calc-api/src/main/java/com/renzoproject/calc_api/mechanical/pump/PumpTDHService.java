package com.renzoproject.calc_api.mechanical.pump;

import com.renzoproject.calc.core.mechanical.pipe.FluidPropertiesResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipePressureLossCalculator;
import com.renzoproject.calc.core.mechanical.pipe.PipeRoughnessResolver;
import com.renzoproject.calc.core.mechanical.pump.PumpTDHCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core. {@link PumpTDHCalculator} itself
 * internally calls {@link PipePressureLossCalculator} — that composition happened in calc-core;
 * this service only wires the plain-Java dependency graph from shared resolvers injected via
 * {@code ResolverConfig}, same pattern as {@code PipePressureLossService}.
 *
 * <p>{@code dimensionResolver} and {@code roughnessResolver} resolve to the same
 * {@code JsonPipeDimensionResolver} bean, which implements both interfaces.
 */
@Service
public class PumpTDHService {

	private final PumpTDHCalculator calculator;

	public PumpTDHService(
			PipeDimensionResolver dimensionResolver,
			PipeRoughnessResolver roughnessResolver,
			FluidPropertiesResolver fluidPropertiesResolver) {
		PipePressureLossCalculator pressureLossCalculator =
				new PipePressureLossCalculator(dimensionResolver, fluidPropertiesResolver, roughnessResolver);
		this.calculator = new PumpTDHCalculator(pressureLossCalculator, fluidPropertiesResolver);
	}

	public PumpTDHResponse calculate(PumpTDHRequest request) {
		var input = PumpTDHMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return PumpTDHMapper.toResponse(result);
	}

}
