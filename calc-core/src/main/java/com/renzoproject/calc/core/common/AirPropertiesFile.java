package com.renzoproject.calc.core.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Shape of {@code reference/common/air-properties.json}. Package-private -- internal to
 * {@link JsonAirPropertiesResolver}'s JSON parsing. {@code description} is present in the JSON
 * but not modeled here and is skipped via {@code @JsonIgnoreProperties}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record AirPropertiesFile(PropertiesSection properties) {

}
