package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.pipe.JsonPipeDimensionResolver;
import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Exposes calc-core's pipe dimension reference data (materials, schedules, nominal sizes)
 * for populating the pipe velocity/sizing calculator's frontend dropdowns.
 *
 * <p>{@link JsonPipeDimensionResolver} is plainly instantiated, same pattern as
 * {@code ConductorReferenceService} and {@code VoltageDropService} — see those classes'
 * Javadoc for why.
 */
@Service
public class PipeReferenceService {

	private final PipeDimensionResolver resolver = new JsonPipeDimensionResolver();

	public List<PipeMaterialDto> listPipeMaterials() {
		return resolver.listAllMaterials().stream().map(PipeMaterialDto::from).toList();
	}

}
