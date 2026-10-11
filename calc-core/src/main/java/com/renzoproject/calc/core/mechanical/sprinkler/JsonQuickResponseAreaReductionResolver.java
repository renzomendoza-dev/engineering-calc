package com.renzoproject.calc.core.mechanical.sprinkler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads {@code reference/sprinkler/quick-response-area-reduction.json} from the classpath once.
 * Mirrors {@link JsonHazardPresetResolver}: fresh {@code ObjectMapper}, hardcoded resource path,
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
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonQuickResponseAreaReductionResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, QuickResponseAreaReductionFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public QuickResponseAreaReductionCurve curve() {
		return curve;
	}

}
