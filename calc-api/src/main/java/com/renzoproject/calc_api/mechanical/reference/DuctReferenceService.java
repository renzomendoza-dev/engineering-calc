package com.renzoproject.calc_api.mechanical.reference;

import com.renzoproject.calc.core.mechanical.duct.DuctRoughnessResolver;
import com.renzoproject.calc.core.mechanical.duct.DuctVelocityLimitResolver;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Exposes calc-core's duct reference tables (velocity limits, material roughness) for display
 * purposes -- so a Duct Sizing caller can pick a sensible {@code maxVelocityMps} default
 * (VELOCITY method) instead of ASHRAE Table 12 values being hardcoded into the frontend, and
 * populate a material dropdown from Table 1 instead of a hardcoded list.
 *
 * <p>Resolvers are the shared singletons from {@code ResolverConfig} -- the roughness resolver is
 * the same instance {@code DuctSizingService} calculates with.
 */
@Service
public class DuctReferenceService {

	private final DuctVelocityLimitResolver velocityLimitResolver;
	private final DuctRoughnessResolver roughnessResolver;

	public DuctReferenceService(DuctVelocityLimitResolver velocityLimitResolver, DuctRoughnessResolver roughnessResolver) {
		this.velocityLimitResolver = velocityLimitResolver;
		this.roughnessResolver = roughnessResolver;
	}

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
