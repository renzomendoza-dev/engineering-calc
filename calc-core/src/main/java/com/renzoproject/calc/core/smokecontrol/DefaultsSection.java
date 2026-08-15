package com.renzoproject.calc.core.smokecontrol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Shape of the {@code defaults} object in {@code defaults.json}. Package-private -- internal to
 * {@link JsonSmokeControlDefaultsResolver}'s JSON parsing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record DefaultsSection(
		DefaultEntry fractionConvectiveHeatInSmokeLayer,
		DefaultEntry convectiveFraction) {

}
