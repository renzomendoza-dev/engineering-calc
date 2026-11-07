package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.List;

/**
 * Loads {@code reference/duct/duct-velocity-limits.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach: read through {@code ReferenceJson}, hardcoded
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
		return ReferenceJson.read(RESOURCE_PATH, DuctVelocityLimitFile.class);
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
