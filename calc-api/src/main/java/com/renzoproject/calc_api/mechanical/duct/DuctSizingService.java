package com.renzoproject.calc_api.mechanical.duct;

import com.renzoproject.calc.core.mechanical.duct.AirPropertiesResolver;
import com.renzoproject.calc.core.mechanical.duct.DuctRoughnessResolver;
import com.renzoproject.calc.core.mechanical.duct.DuctSizingCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core. Resolvers are shared singletons
 * injected from {@code ResolverConfig} (the roughness resolver is the same instance
 * {@code DuctReferenceService} displays); the calculator is plainly constructed.
 *
 * <p>No exception handling here -- {@code CalculationException} (unknown material, bisection
 * non-convergence, non-physical rectangular dimension) propagates straight through to the
 * existing global handler, same single-exception-type discipline as every other service. calc-core's
 * own throw sites already give each failure mode a distinct message (a "does not bracket a root"
 * / "did not converge" message from the bisection solver reads differently from a "must be
 * positive" validation message), so no rewrapping is needed here to satisfy that distinction.
 */
@Service
public class DuctSizingService {

	private final DuctSizingCalculator calculator;

	public DuctSizingService(AirPropertiesResolver airPropertiesResolver, DuctRoughnessResolver roughnessResolver) {
		this.calculator = new DuctSizingCalculator(airPropertiesResolver, roughnessResolver);
	}

	public DuctSizingResponse calculate(DuctSizingRequest request) {
		var input = DuctSizingMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return DuctSizingMapper.toResponse(result);
	}

}
