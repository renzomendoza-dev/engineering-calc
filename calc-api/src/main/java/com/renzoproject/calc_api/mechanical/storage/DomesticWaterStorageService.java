package com.renzoproject.calc_api.mechanical.storage;

import com.renzoproject.calc.core.mechanical.storage.DomesticWaterStorageCalculator;
import com.renzoproject.calc.core.mechanical.storage.JsonFixtureUnitDemandResolver;
import com.renzoproject.calc.core.mechanical.storage.JsonPerCapitaConsumptionResolver;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code FirePumpPowerService}: {@link DomesticWaterStorageCalculator} and its two resolver
 * dependencies are plainly instantiated rather than Spring beans, since all three are stateless,
 * dependency-free POJOs from calc-core.
 */
@Service
public class DomesticWaterStorageService {

	private final DomesticWaterStorageCalculator calculator =
			new DomesticWaterStorageCalculator(new JsonPerCapitaConsumptionResolver(), new JsonFixtureUnitDemandResolver());

	public DomesticWaterStorageResponse calculate(DomesticWaterStorageRequest request) {
		var input = DomesticWaterStorageMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return DomesticWaterStorageMapper.toResponse(result);
	}

}
