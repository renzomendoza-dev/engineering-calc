package com.renzoproject.calc_api.mechanical.tank;

import com.renzoproject.calc.core.mechanical.pipe.JsonFluidPropertiesResolver;
import com.renzoproject.calc.core.mechanical.tank.ExpansionTankCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code DuctSizingService}: {@link ExpansionTankCalculator} and its
 * {@code JsonFluidPropertiesResolver} dependency are plainly instantiated rather than Spring
 * beans, since both are stateless, dependency-free POJOs from calc-core.
 *
 * <p>No exception handling here -- {@code CalculationException} (hot temp &lt;= cold temp, max
 * operating pressure &lt;= fill pressure, water temperature outside the fluid resolver's 0-100
 * degC table range) propagates straight through to the existing global handler, same
 * single-exception-type discipline as every other service. All three of those failure modes come
 * through as the same exception type with calc-core's own distinct messages -- good enough to
 * distinguish them in a log or an error-detail field today, but if a future frontend needs to
 * branch on *which* rule failed (rather than just displaying the message), calc-core would need
 * more specific exception subtypes; not attempted here since it's a broader cross-cutting change
 * outside this endpoint's scope.
 */
@Service
public class ExpansionTankService {

	private final ExpansionTankCalculator calculator = new ExpansionTankCalculator(new JsonFluidPropertiesResolver());

	public ExpansionTankResponse calculate(ExpansionTankRequest request) {
		var input = ExpansionTankMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return ExpansionTankMapper.toResponse(result);
	}

}
