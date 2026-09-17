package com.renzoproject.calc_api.mechanical.storage;

import com.renzoproject.calc.core.mechanical.storage.FireWaterDurationResolver;
import com.renzoproject.calc.core.mechanical.storage.FireWaterStorageCalculator;
import org.springframework.stereotype.Service;

/**
 * Thin orchestration between the web layer and calc-core. The duration resolver is a shared
 * singleton injected from {@code ResolverConfig} (the same instance
 * {@code StorageReferenceService} displays); the calculator is plainly constructed. No
 * dependency on {@code DomesticWaterStorageService} -- preserves the decoupling decided at the
 * core layer.
 */
@Service
public class FireWaterStorageService {

	private final FireWaterStorageCalculator calculator;

	public FireWaterStorageService(FireWaterDurationResolver durationResolver) {
		this.calculator = new FireWaterStorageCalculator(durationResolver);
	}

	public FireWaterStorageResponse calculate(FireWaterStorageRequest request) {
		var input = FireWaterStorageMapper.toCoreInput(request);
		var result = calculator.calculate(input);
		return FireWaterStorageMapper.toResponse(result);
	}

}
