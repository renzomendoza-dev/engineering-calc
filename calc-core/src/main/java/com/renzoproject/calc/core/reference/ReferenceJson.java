package com.renzoproject.calc.core.reference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads a reference data file from the classpath. Every JSON resolver loads its tables through here,
 * once, when it is built: one {@link ObjectMapper} (thread-safe once configured, and it is never
 * reconfigured) and the same two errors for a missing or unreadable file.
 */
public final class ReferenceJson {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ReferenceJson() {
	}

	/**
	 * @param resourcePath absolute classpath path, e.g. {@code /reference/pipes/bi.json}
	 * @param type         the record (or array of records) the file holds
	 * @throws CalculationException if the file is missing or cannot be read as {@code type}
	 */
	public static <T> T read(String resourcePath, Class<T> type) {
		try (InputStream in = ReferenceJson.class.getResourceAsStream(resourcePath)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + resourcePath);
			}
			return MAPPER.readValue(in, type);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + resourcePath, e);
		}
	}

}
