package com.renzoproject.calc.core.mechanical.duct;

import com.renzoproject.calc.core.exception.CalculationException;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads {@code reference/duct/duct-roughness.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach: read through {@code ReferenceJson}, hardcoded
 * resource path constant, {@code CalculationException} on a missing/unreadable resource, parsed
 * eagerly in the constructor and cached rather than re-read per call.
 */
public class JsonDuctRoughnessResolver implements DuctRoughnessResolver {

	private static final String RESOURCE_PATH = "/reference/duct/duct-roughness.json";

	private final List<DuctRoughnessRow> rows;
	private final Map<String, Double> roughnessByMaterial;

	public JsonDuctRoughnessResolver() {
		DuctRoughnessFile file = load();
		this.rows = file.materials();
		this.roughnessByMaterial = file.materials().stream()
				.collect(Collectors.toMap(DuctRoughnessRow::material, DuctRoughnessRow::absoluteRoughnessMm));
	}

	private static DuctRoughnessFile load() {
		return ReferenceJson.read(RESOURCE_PATH, DuctRoughnessFile.class);
	}

	@Override
	public double resolveAbsoluteRoughnessMm(String material) {
		if (material == null) {
			throw new CalculationException("material is required");
		}
		Double roughness = roughnessByMaterial.get(material);
		if (roughness == null) {
			throw new CalculationException("Unknown duct material: " + material
					+ " (known materials: " + roughnessByMaterial.keySet() + ")");
		}
		return roughness;
	}

	@Override
	public List<DuctRoughnessRow> allEntries() {
		return rows;
	}

}
