package com.renzoproject.calc.core.smokecontrol;

import com.renzoproject.calc.core.reference.ReferenceJson;

/**
 * Loads {@code reference/smoke-control/defaults.json} from the classpath once. Mirrors
 * {@code JsonFirePumpMotorSizeResolver}'s loading approach: read through {@code ReferenceJson},
 * hardcoded resource path constant, {@code CalculationException} on a missing/unreadable
 * resource, parsed eagerly in the constructor and cached rather than re-read per call.
 */
public class JsonSmokeControlDefaultsResolver implements SmokeControlDefaultsResolver {

	private static final String RESOURCE_PATH = "/reference/smoke-control/defaults.json";

	private final SmokeControlDefaults defaults;

	public JsonSmokeControlDefaultsResolver() {
		SmokeControlDefaultsFile file = load();
		this.defaults = new SmokeControlDefaults(
				file.defaults().fractionConvectiveHeatInSmokeLayer().value(),
				file.defaults().convectiveFraction().value(),
				file.defaults().ventDischargeCoefficient().value());
	}

	private static SmokeControlDefaultsFile load() {
		return ReferenceJson.read(RESOURCE_PATH, SmokeControlDefaultsFile.class);
	}

	@Override
	public SmokeControlDefaults defaults() {
		return defaults;
	}

}
