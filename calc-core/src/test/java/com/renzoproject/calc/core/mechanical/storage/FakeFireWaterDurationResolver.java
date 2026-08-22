package com.renzoproject.calc.core.mechanical.storage;

import java.util.List;
import java.util.Map;

class FakeFireWaterDurationResolver implements FireWaterDurationResolver {

	private final Map<HazardClassification, DurationRange> rangesByClassification;

	FakeFireWaterDurationResolver(Map<HazardClassification, DurationRange> rangesByClassification) {
		this.rangesByClassification = rangesByClassification;
	}

	@Override
	public DurationRange resolve(HazardClassification classification) {
		return rangesByClassification.get(classification);
	}

	@Override
	public List<FireWaterDurationEntry> allEntries() {
		throw new UnsupportedOperationException("Not needed by any test using this fake");
	}

}
