package com.renzoproject.calc.core.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Shape of the {@code properties} object in {@code air-properties.json}. Package-private --
 * internal to {@link JsonAirPropertiesResolver}'s JSON parsing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record PropertiesSection(
		AirPropertyEntry specificHeat,
		AirPropertyEntry atmosphericPressure,
		AirPropertyEntry specificGasConstant) {

}
