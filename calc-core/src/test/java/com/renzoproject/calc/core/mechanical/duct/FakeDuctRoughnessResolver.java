package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;
import java.util.Map;

class FakeDuctRoughnessResolver implements DuctRoughnessResolver {

	private final Map<String, Double> roughnessByMaterial;

	FakeDuctRoughnessResolver(Map<String, Double> roughnessByMaterial) {
		this.roughnessByMaterial = roughnessByMaterial;
	}

	@Override
	public double resolveAbsoluteRoughnessMm(String material) {
		Double roughness = roughnessByMaterial.get(material);
		if (roughness == null) {
			throw new CalculationException("Fake resolver: unknown duct material " + material);
		}
		return roughness;
	}

	@Override
	public List<DuctRoughnessRow> allEntries() {
		throw new UnsupportedOperationException("Not needed by any test using this fake");
	}

}
