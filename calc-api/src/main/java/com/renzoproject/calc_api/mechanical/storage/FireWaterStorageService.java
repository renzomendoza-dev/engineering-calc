package com.renzoproject.calc_api.mechanical.storage;

import com.renzoproject.calc.core.mechanical.storage.FireWaterStorageCalculator;
import com.renzoproject.calc.core.mechanical.storage.JsonFireWaterDurationResolver;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core, same pattern as
 * {@code FirePumpPowerService}: {@link FireWaterStorageCalculator} and its resolver dependency
 * are plainly instantiated rather than Spring beans, since both are stateless, dependency-free
 * POJOs from calc-core. No dependency on {@code DomesticWaterStorageService} -- preserves the
 * decoupling decided at the core layer.
 */
@Service
public class FireWaterStorageService {

	private final FireWaterStorageCalculator calculator = new FireWaterStorageCalculator(new JsonFireWaterDurationResolver());

	public FireWaterStorageResponse calculate(FireWaterStorageRequest request) {
		var input = FireWaterStorageMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return FireWaterStorageMapper.toResponse(result);
	}

}
