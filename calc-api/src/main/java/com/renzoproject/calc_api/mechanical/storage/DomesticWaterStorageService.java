package com.renzoproject.calc_api.mechanical.storage;

import com.renzoproject.calc.core.mechanical.storage.DomesticWaterStorageCalculator;
import com.renzoproject.calc.core.mechanical.storage.FixtureUnitDemandResolver;
import com.renzoproject.calc.core.mechanical.storage.PerCapitaConsumptionResolver;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core. Resolvers are shared singletons
 * injected from {@code ResolverConfig} (the same instances {@code StorageReferenceService}
 * displays); the calculator is plainly constructed.
 */
@Service
public class DomesticWaterStorageService {

	private final DomesticWaterStorageCalculator calculator;

	public DomesticWaterStorageService(
			PerCapitaConsumptionResolver perCapitaConsumptionResolver,
			FixtureUnitDemandResolver fixtureUnitDemandResolver) {
		this.calculator = new DomesticWaterStorageCalculator(perCapitaConsumptionResolver, fixtureUnitDemandResolver);
	}

	public DomesticWaterStorageResponse calculate(DomesticWaterStorageRequest request) {
		var input = DomesticWaterStorageMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return DomesticWaterStorageMapper.toResponse(result);
	}

}
