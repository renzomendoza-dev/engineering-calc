package com.renzoproject.calc.core.mechanical.firepump;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzoproject.calc.core.exception.CalculationException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads {@code reference/firepump/curve-requirements.json} from the classpath once. Mirrors
 * {@code JsonPipeDimensionResolver}'s loading approach.
 */
public class JsonFirePumpCurveRequirementsLoader implements FirePumpCurveRequirementsLoader {

	private static final String RESOURCE_PATH = "/reference/firepump/curve-requirements.json";

	private final FirePumpCurveRequirements requirements;

	public JsonFirePumpCurveRequirementsLoader() {
		this.requirements = loadRequirements();
	}

	private static FirePumpCurveRequirements loadRequirements() {
		ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream in = JsonFirePumpCurveRequirementsLoader.class.getResourceAsStream(RESOURCE_PATH)) {
			if (in == null) {
				throw new CalculationException("Reference data resource not found: " + RESOURCE_PATH);
			}
			CurveRequirementsFile file = objectMapper.readValue(in, CurveRequirementsFile.class);
			return new FirePumpCurveRequirements(
					file.churnMaxPercentOfRated(), file.overloadFlowPercentOfRated(), file.overloadMinPressurePercentOfRated());
		} catch (IOException e) {
			throw new CalculationException("Failed to load reference data: " + RESOURCE_PATH, e);
		}
	}

	@Override
	public FirePumpCurveRequirements load() {
		return requirements;
	}

}
