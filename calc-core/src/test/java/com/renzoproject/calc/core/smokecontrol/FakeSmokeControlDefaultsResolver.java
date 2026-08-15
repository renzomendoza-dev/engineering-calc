package com.renzoproject.calc.core.smokecontrol;

class FakeSmokeControlDefaultsResolver implements SmokeControlDefaultsResolver {

	private final SmokeControlDefaults defaults;

	FakeSmokeControlDefaultsResolver(SmokeControlDefaults defaults) {
		this.defaults = defaults;
	}

	@Override
	public SmokeControlDefaults defaults() {
		return defaults;
	}

}
