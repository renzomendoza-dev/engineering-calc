package com.renzoproject.calc.core.mechanical.sprinkler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;

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
				file.minimumOperatingPressurePsi(),
				file.minimumOperatingPressureNote());
	}

	private static RemoteAreaRulesFile load() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonRemoteAreaRulesResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, RemoteAreaRulesFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public RemoteAreaRules rules() {
		return rules;
	}

}
