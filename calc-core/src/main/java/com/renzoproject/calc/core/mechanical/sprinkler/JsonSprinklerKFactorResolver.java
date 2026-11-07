package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.List;

/**
 * Loads {@code reference/sprinkler/sprinkler-k-factors.json} from the classpath once, same loading
 * approach as {@link JsonHazardPresetResolver}.
 */
public class JsonSprinklerKFactorResolver implements SprinklerKFactorResolver {

	private static final String RESOURCE_PATH = "/reference/sprinkler/sprinkler-k-factors.json";

	private final List<SprinklerKFactorRow> rows;

	public JsonSprinklerKFactorResolver() {
		this.rows = load().kFactors();
	}

	private static SprinklerKFactorFile load() {
		return ReferenceJson.read(RESOURCE_PATH, SprinklerKFactorFile.class);
	}

	@Override
	public List<SprinklerKFactorRow> allEntries() {
		return rows;
	}

}
