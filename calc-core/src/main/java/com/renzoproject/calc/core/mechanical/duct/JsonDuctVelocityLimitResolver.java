package com.renzoproject.calc.core.mechanical.duct;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads {@code reference/duct/duct-velocity-limits.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach: fresh {@code ObjectMapper}, hardcoded
 * resource path constant, {@code CalculationException} on a missing/unreadable resource, parsed
 * eagerly in the constructor and cached rather than re-read per call.
 */
public class JsonDuctVelocityLimitResolver implements DuctVelocityLimitResolver {

	private static final String RESOURCE_PATH = "/reference/duct/duct-velocity-limits.json";

	private final List<DuctVelocityLimitRow> limits;

	public JsonDuctVelocityLimitResolver() {
		this.limits = load().limits();
	}

	private static DuctVelocityLimitFile load() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonDuctVelocityLimitResolver.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			return objectMapper.readValue(in, DuctVelocityLimitFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public double resolveMaxVelocity(String ductLocation, int ncRcRating, DuctShape shape) {
		if (ductLocation == null) {
			throw new CalculationException("ductLocation is required");
		}
		return limits.stream()
				.filter(row -> row.ductLocation().equals(ductLocation) && row.ncRcRating() == ncRcRating)
				.findFirst()
				.map(row -> shape == DuctShape.ROUND ? row.maxVelocityRound() : row.maxVelocityRectangular())
				.orElseThrow(() -> new CalculationException("No velocity limit data for ductLocation=" + ductLocation
						+ ", ncRcRating=" + ncRcRating));
	}

	@Override
	public List<DuctVelocityLimitRow> allEntries() {
		return limits;
	}

}
