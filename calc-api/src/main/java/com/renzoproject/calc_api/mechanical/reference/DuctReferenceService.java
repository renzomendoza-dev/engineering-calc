package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.duct.DuctRoughnessResolver;
import com.renzoproject.calc.core.mechanical.duct.DuctVelocityLimitResolver;
import com.renzoproject.calc.core.mechanical.duct.JsonDuctRoughnessResolver;
import com.renzoproject.calc.core.mechanical.duct.JsonDuctVelocityLimitResolver;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Exposes calc-core's duct reference tables (velocity limits, material roughness) for display
 * purposes -- so a Duct Sizing caller can pick a sensible {@code maxVelocityMps} default
 * (VELOCITY method) instead of ASHRAE Table 12 values being hardcoded into the frontend, and
 * populate a material dropdown from Table 1 instead of a hardcoded list. Resolvers are plainly
 * instantiated, same pattern as {@code PipeReferenceService}/{@code StorageReferenceService}.
 */
@Service
public class DuctReferenceService {

	private final DuctVelocityLimitResolver velocityLimitResolver = new JsonDuctVelocityLimitResolver();
	private final DuctRoughnessResolver roughnessResolver = new JsonDuctRoughnessResolver();

	/** Raw rows of reference/duct/duct-velocity-limits.json, for display purposes. */
	public List<DuctVelocityLimitEntryDto> listVelocityLimits() {
		return velocityLimitResolver.allEntries().stream()
				.map(DuctVelocityLimitEntryDto::from)
				.toList();
	}

	/** Raw rows of reference/duct/duct-roughness.json, for populating a material dropdown. */
	public List<DuctRoughnessEntryDto> listDuctRoughnessTable() {
		return roughnessResolver.allEntries().stream()
				.map(DuctRoughnessEntryDto::from)
				.toList();
	}

}
