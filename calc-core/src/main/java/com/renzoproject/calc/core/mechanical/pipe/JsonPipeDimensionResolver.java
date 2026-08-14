package com.renzoproject.calc.core.mechanical.pipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads pipe dimension reference data from {@code reference/pipes/{material}.json} on the
 * classpath, one file per material — same eager-load-into-a-combined-map approach as
 * {@code MotorFlcTable} in the electrical package (fresh {@code ObjectMapper} per file, for
 * consistency with that class's style).
 *
 * <p>Material codes are hardcoded rather than discovering files by scanning the classpath
 * directory: every other reference-data loader in this codebase (electrical or otherwise)
 * hardcodes its resource path(s) as constants, and mirroring that established, already-proven
 * pattern was explicitly requested over introducing a new loading approach. Adding a fifth
 * material means adding one more entry to {@link #RESOURCE_PATHS_BY_MATERIAL} — a one-line
 * change, not a structural one.
 */
public class JsonPipeDimensionResolver implements PipeDimensionResolver, PipeRoughnessResolver {

	private static final Unit<Length> MILLIMETRE = MetricPrefix.MILLI(Units.METRE);

	private static final Map<String, String> RESOURCE_PATHS_BY_MATERIAL = Map.of(
			"GI", "/reference/pipes/gi.json",
			"BI", "/reference/pipes/bi.json",
			"UPVC", "/reference/pipes/upvc.json",
			"PPR", "/reference/pipes/ppr.json");

	/**
	 * Fixed display order for {@link #listAllMaterials()} — {@link #RESOURCE_PATHS_BY_MATERIAL}
	 * is a {@code Map.of()} with no guaranteed iteration order, but a dropdown needs a stable one.
	 */
	private static final List<String> MATERIAL_ORDER = List.of("GI", "BI", "UPVC", "PPR");

	private final Map<String, PipeDimensionFile> filesByMaterial;

	public JsonPipeDimensionResolver() {
		Map<String, PipeDimensionFile> loaded = new HashMap<>();
		for (Map.Entry<String, String> entry : RESOURCE_PATHS_BY_MATERIAL.entrySet()) {
			loaded.put(entry.getKey(), loadFile(entry.getValue()));
		}
		this.filesByMaterial = loaded;
	}

	private static PipeDimensionFile loadFile(String resourcePath) {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonPipeDimensionResolver.class.getResourceAsStream(resourcePath)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + resourcePath);
			}
			return objectMapper.readValue(in, PipeDimensionFile.class);
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + resourcePath, e);
		}
	}

	@Override
	public PipeDimension resolve(String material, String schedule, String nominalLabel) {
		List<PipeSizeRow> rows = rowsFor(material, schedule);
		return rows.stream()
				.filter(row -> row.nominalSize().equalsIgnoreCase(nominalLabel))
				.findFirst()
				.map(JsonPipeDimensionResolver::toDimension)
				.orElseThrow(() -> new CalculationException("No pipe dimension data for material=" + material
						+ ", schedule=" + schedule + ", nominalSize=" + nominalLabel));
	}

	@Override
	public PipeDimension resolveNextStandardSize(Quantity<Length> calculatedMinDiameter, String material, String schedule) {
		if (calculatedMinDiameter == null) {
			throw new CalculationException("calculatedMinDiameter is required");
		}
		double minMm = calculatedMinDiameter.to(MILLIMETRE).getValue().doubleValue();
		List<PipeSizeRow> rows = rowsFor(material, schedule);

		return rows.stream()
				.filter(row -> row.internalDiameterMm() >= minMm)
				.min(Comparator.comparingDouble(PipeSizeRow::internalDiameterMm))
				.map(JsonPipeDimensionResolver::toDimension)
				.orElseThrow(() -> {
					double largest = rows.stream().mapToDouble(PipeSizeRow::internalDiameterMm).max().orElse(Double.NaN);
					return new CalculationException("No standard pipe size for material=" + material
							+ ", schedule=" + schedule + " has an internal diameter >= " + minMm
							+ " mm (largest available: " + largest + " mm)");
				});
	}

	@Override
	public double resolveAbsoluteRoughnessMm(String material) {
		PipeDimensionFile file = fileFor(material);
		if (file.hydraulics() == null) {
			throw new CalculationException("No hydraulics data for material: " + material);
		}
		return file.hydraulics().absoluteRoughnessMm();
	}

	@Override
	public List<PipeMaterialReference> listAllMaterials() {
		return MATERIAL_ORDER.stream()
				.map(filesByMaterial::get)
				.map(JsonPipeDimensionResolver::toMaterialReference)
				.toList();
	}

	private static PipeMaterialReference toMaterialReference(PipeDimensionFile file) {
		List<PipeScheduleReference> schedules = file.schedules().stream()
				.map(JsonPipeDimensionResolver::toScheduleReference)
				.toList();
		return new PipeMaterialReference(file.material(), file.materialName(), file.confidence(), schedules);
	}

	private static PipeScheduleReference toScheduleReference(PipeScheduleGroup group) {
		List<PipeSizeReference> sizes = group.sizes().stream()
				.map(row -> new PipeSizeReference(row.nominalSize(), row.nominalLabel(), row.internalDiameterMm()))
				.toList();
		return new PipeScheduleReference(group.schedule(), sizes);
	}

	private List<PipeSizeRow> rowsFor(String material, String schedule) {
		PipeDimensionFile file = fileFor(material);
		List<PipeScheduleGroup> matchingGroups = file.schedules().stream()
				.filter(group -> schedule != null && group.schedule().equalsIgnoreCase(schedule))
				.toList();
		if (matchingGroups.isEmpty()) {
			List<String> available = file.schedules().stream().map(PipeScheduleGroup::schedule).toList();
			throw new CalculationException("Unknown schedule '" + schedule + "' for material " + material
					+ " (available schedules: " + available + ")");
		}
		return matchingGroups.stream().flatMap(group -> group.sizes().stream()).toList();
	}

	private PipeDimensionFile fileFor(String material) {
		if (material == null) {
			throw new CalculationException("material is required");
		}
		PipeDimensionFile file = filesByMaterial.get(material.toUpperCase());
		if (file == null) {
			throw new CalculationException("Unknown pipe material: " + material
					+ " (known materials: " + filesByMaterial.keySet() + ")");
		}
		return file;
	}

	private static PipeDimension toDimension(PipeSizeRow row) {
		return new PipeDimension(
				row.nominalSize(),
				row.nominalLabel(),
				Quantities.getQuantity(row.internalDiameterMm(), MILLIMETRE),
				Quantities.getQuantity(row.outsideDiameterMm(), MILLIMETRE));
	}

}
