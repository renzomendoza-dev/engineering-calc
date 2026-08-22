package com.renzoproject.calc.core.mechanical.storage;

import com.renzoproject.calc.core.exception.CalculationException;

import java.util.List;
import java.util.Map;

class FakePerCapitaConsumptionResolver implements PerCapitaConsumptionResolver {

	private final Map<String, Double> lpcdByType;

	FakePerCapitaConsumptionResolver(Map<String, Double> lpcdByType) {
		this.lpcdByType = lpcdByType;
	}

	@Override
	public double resolveLpcd(String occupancyType) {
		Double lpcd = lpcdByType.get(occupancyType);
		if (lpcd == null) {
			throw new CalculationException("Fake resolver: unknown occupancy type " + occupancyType);
		}
		return lpcd;
	}

	@Override
	public List<OccupancyTypeRow> allEntries() {
		throw new UnsupportedOperationException("Not needed by any test using this fake");
	}

}
