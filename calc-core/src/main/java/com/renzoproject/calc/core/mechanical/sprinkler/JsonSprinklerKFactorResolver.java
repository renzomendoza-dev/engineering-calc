package com.renzoproject.calc.core.mechanical.sprinkler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
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
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonSprinklerKFactorResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, SprinklerKFactorFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public List<SprinklerKFactorRow> allEntries() {
		return rows;
	}

}
