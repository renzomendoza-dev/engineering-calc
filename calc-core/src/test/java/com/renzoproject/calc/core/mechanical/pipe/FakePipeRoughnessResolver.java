package com.renzoproject.calc.core.mechanical.pipe;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.Map;

/**
 * Minimal in-memory {@link PipeRoughnessResolver} for {@link PipePressureLossCalculatorTest},
 * so it isn't coupled to real reference data values.
 */
class FakePipeRoughnessResolver implements PipeRoughnessResolver {

	private final Map<String, Double> roughnessMmByMaterial;

	FakePipeRoughnessResolver(Map<String, Double> roughnessMmByMaterial) {
		this.roughnessMmByMaterial = roughnessMmByMaterial;
	}

	@Override
	public double resolveAbsoluteRoughnessMm(String material) {
		Double roughness = roughnessMmByMaterial.get(material);
		if (roughness == null) {
			throw new CalculationException("Fake resolver: no roughness for material " + material);
		}
		return roughness;
	}

}
