package com.renzoproject.calc.core.mechanical.sprinkler;

import com.renzoproject.calc.core.exception.CalculationException;

import com.renzoproject.calc.core.reference.ReferenceJson;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code reference/sprinkler/hazard-presets.json} from the classpath once. Mirrors
 * {@code JsonFireWaterDurationResolver}: read through {@code ReferenceJson}, hardcoded resource path
 * constant, {@code CalculationException} on a missing/unreadable resource, parsed eagerly in the
 * constructor and cached rather than re-read per call.
 */
public class JsonHazardPresetResolver implements HazardPresetResolver {

	private static final String RESOURCE_PATH = "/reference/sprinkler/hazard-presets.json";

	private final Map<SprinklerHazard, HazardPresetRow> presetsByHazard = new EnumMap<>(SprinklerHazard.class);
	private final List<HazardPresetRow> presets;

	public JsonHazardPresetResolver() {
		HazardPresetFile file = load();
		this.presets = file.presets();
		for (HazardPresetRow row : file.presets()) {
			presetsByHazard.put(parseHazard(row.hazard()), row);
		}
	}

	private static HazardPresetFile load() {
		return ReferenceJson.read(RESOURCE_PATH, HazardPresetFile.class);
	}

	private static SprinklerHazard parseHazard(String rawValue) {
		try {
			return SprinklerHazard.valueOf(rawValue);
		} catch (IllegalArgumentException e) {
			throw new CalculationException("Unknown hazard in " + RESOURCE_PATH + ": " + rawValue);
		}
	}

	@Override
	public HazardPresetRow resolve(SprinklerHazard hazard) {
		if (hazard == null) {
			throw new CalculationException("hazard is required");
		}
		HazardPresetRow row = presetsByHazard.get(hazard);
		if (row == null) {
			throw new CalculationException("No published design preset for hazard: " + hazard);
		}
		return row;
	}

	@Override
	public List<HazardPresetRow> allEntries() {
		return presets;
	}

}
