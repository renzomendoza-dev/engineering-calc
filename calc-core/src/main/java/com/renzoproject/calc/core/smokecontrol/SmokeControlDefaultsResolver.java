package com.renzoproject.calc.core.smokecontrol;

/**
 * Resolves smoke-control-specific default assumptions (Ks, chi) for
 * {@link SmokeProductionCalculator}, used whenever {@link SmokeProductionInput} leaves the
 * corresponding field unsupplied. This is a pre-step that runs before any calculation -- it does
 * no math of its own, mirroring {@code ConductorPropertiesResolver}'s separation of
 * reference-data lookup from calculation.
 */
public interface SmokeControlDefaultsResolver {

	SmokeControlDefaults defaults();

}
