package com.renzoproject.calc.core.mechanical.firepump;

import com.renzoproject.calc.core.reference.ReferenceJson;

/**
 * Loads {@code reference/firepump/curve-requirements.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach.
 */
public class JsonFirePumpCurveRequirementsLoader implements FirePumpCurveRequirementsLoader {

	private static final String RESOURCE_PATH = "/reference/firepump/curve-requirements.json";

	private final FirePumpCurveRequirements requirements;

	public JsonFirePumpCurveRequirementsLoader() {
		this.requirements = loadRequirements();
	}

	private static FirePumpCurveRequirements loadRequirements() {
		CurveRequirementsFile file = ReferenceJson.read(RESOURCE_PATH, CurveRequirementsFile.class);
		return new FirePumpCurveRequirements(
				file.churnMaxPercentOfRated(), file.overloadFlowPercentOfRated(), file.overloadMinPressurePercentOfRated());
	}

	@Override
	public FirePumpCurveRequirements load() {
		return requirements;
	}

}
