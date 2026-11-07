package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.reference.ReferenceJson;

/**
 * Loads {@code reference/sprinkler/remote-area-rules.json} from the classpath once. Mirrors
 * {@link JsonHazardPresetResolver}.
 */
public class JsonRemoteAreaRulesResolver implements RemoteAreaRulesResolver {

	private static final String RESOURCE_PATH = "/reference/sprinkler/remote-area-rules.json";

	private final RemoteAreaRules rules;

	public JsonRemoteAreaRulesResolver() {
		RemoteAreaRulesFile file = load();
		this.rules = new RemoteAreaRules(
				file.standard(),
				file.confidence(),
				file.remoteAreaLengthFactor(),
				file.remoteAreaLengthNote(),
				file.remoteAreaLengthConfidence(),
				file.remoteAreaLengthSource(),
				file.minimumOperatingPressurePsi(),
				file.minimumOperatingPressureNote(),
				file.minimumOperatingPressureConfidence(),
				file.minimumOperatingPressureSource(),
				file.maximumOperatingPressurePsi(),
				file.maximumOperatingPressureSource(),
				file.maximumOperatingPressureNote());
	}

	private static RemoteAreaRulesFile load() {
		return ReferenceJson.read(RESOURCE_PATH, RemoteAreaRulesFile.class);
	}

	@Override
	public RemoteAreaRules rules() {
		return rules;
	}

}
