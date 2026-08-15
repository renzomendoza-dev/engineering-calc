package com.renzoproject.calc.core.common;

/**
 * Resolves general air/combustion-gas physical properties. This is a pre-step that runs before
 * any calculation -- it does no math of its own, mirroring {@code ConductorPropertiesResolver}'s
 * separation of reference-data lookup from calculation.
 */
public interface AirPropertiesResolver {

	AirProperties properties();

}
