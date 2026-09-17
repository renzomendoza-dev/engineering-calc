package com.renzoproject.calc.core.mechanical.storage;

import java.util.List;
import java.util.Map;

class FakeFixtureUnitDemandResolver implements FixtureUnitDemandResolver {

	private final Map<FlushSystemType, Double> gpmBySystemType;

	FakeFixtureUnitDemandResolver(Map<FlushSystemType, Double> gpmBySystemType) {
		this.gpmBySystemType = gpmBySystemType;
	}

	@Override
	public double resolveGpm(double totalWsfu, FlushSystemType systemType) {
		return gpmBySystemType.get(systemType);
	}

	@Override
	public List<WsfuDemandRow> allEntries() {
		throw new UnsupportedOperationException("Not needed by any test using this fake");
	}

}
