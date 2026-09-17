package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.pipe.PipeDimensionResolver;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Exposes calc-core's pipe dimension reference data (materials, schedules, nominal sizes)
 * for populating the pipe velocity/sizing calculator's frontend dropdowns.
 *
 * <p>The resolver is the shared singleton from {@code ResolverConfig} -- the same instance the
 * pipe and pump calculators use.
 */
@Service
public class PipeReferenceService {

	private final PipeDimensionResolver resolver;

	public PipeReferenceService(PipeDimensionResolver resolver) {
		this.resolver = resolver;
	}

	public List<PipeMaterialDto> listPipeMaterials() {
		return resolver.listAllMaterials().stream().map(PipeMaterialDto::from).toList();
	}

}
