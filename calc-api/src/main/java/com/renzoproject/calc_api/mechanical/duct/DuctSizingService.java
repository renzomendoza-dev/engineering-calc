package com.renzoproject.calc_api.mechanical.duct;

import com.renzoproject.calc.core.mechanical.duct.AnalyticalAirPropertiesResolver;
import com.renzoproject.calc.core.mechanical.duct.DuctSizingCalculator;
import com.renzoproject.calc.core.mechanical.duct.JsonDuctRoughnessResolver;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code PipePressureLossService}: {@link DuctSizingCalculator} and its two dependencies are
 * plainly instantiated rather than Spring beans, since all three are stateless, dependency-free
 * POJOs from calc-core.
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

	private final DuctSizingCalculator calculator =
			new DuctSizingCalculator(new AnalyticalAirPropertiesResolver(), new JsonDuctRoughnessResolver());

	public DuctSizingResponse calculate(DuctSizingRequest request) {
		var input = DuctSizingMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return DuctSizingMapper.toResponse(result);
	}

}
