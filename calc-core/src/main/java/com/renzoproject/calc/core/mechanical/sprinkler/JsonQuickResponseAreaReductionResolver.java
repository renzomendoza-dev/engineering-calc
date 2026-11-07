package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.reference.ReferenceJson;

/**
 * Loads {@code reference/sprinkler/quick-response-area-reduction.json} from the classpath once.
 * Mirrors {@link JsonHazardPresetResolver}: read through {@code ReferenceJson}, hardcoded resource path,
 * {@code CalculationException} on a missing or unreadable resource, parsed eagerly and cached.
 */
public class JsonQuickResponseAreaReductionResolver implements QuickResponseAreaReductionResolver {

	private static final String RESOURCE_PATH = "/reference/sprinkler/quick-response-area-reduction.json";

	private final QuickResponseAreaReductionCurve curve;

	public JsonQuickResponseAreaReductionResolver() {
		QuickResponseAreaReductionFile file = load();
		this.curve = new QuickResponseAreaReductionCurve(
				file.standard(),
				file.confidence(),
				file.conditionsConfidence(),
				file.conditionsNote(),
				file.lowCeilingFt(),
				file.highCeilingFt(),
				file.reductionBelowLowCeilingPercent(),
				file.reductionAboveHighCeilingPercent(),
				file.slopePercentPerFt(),
				file.interceptPercent());
	}

	private static QuickResponseAreaReductionFile load() {
		return ReferenceJson.read(RESOURCE_PATH, QuickResponseAreaReductionFile.class);
	}

	@Override
	public QuickResponseAreaReductionCurve curve() {
		return curve;
	}

}
