package com.renzoproject.calc.core.mechanical.firepump;

class FakeFirePumpCurveRequirementsLoader implements FirePumpCurveRequirementsLoader {

	private final FirePumpCurveRequirements requirements;

	FakeFirePumpCurveRequirementsLoader(FirePumpCurveRequirements requirements) {
		this.requirements = requirements;
	}

	@Override
	public FirePumpCurveRequirements load() {
		return requirements;
	}

}
