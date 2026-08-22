package com.renzoproject.calc.core.mechanical.storage;

import java.util.List;
import java.util.Map;

class FakeFixtureUnitDemandResolver implements FixtureUnitDemandResolver {

	private final Map<SystemType, Double> gpmBySystemType;

	FakeFixtureUnitDemandResolver(Map<SystemType, Double> gpmBySystemType) {
		this.gpmBySystemType = gpmBySystemType;
	}

	@Override
	public double resolveGpm(double totalWsfu, SystemType systemType) {
		return gpmBySystemType.get(systemType);
	}

	@Override
	public List<WsfuDemandRow> allEntries() {
		throw new UnsupportedOperationException("Not needed by any test using this fake");
	}

}
