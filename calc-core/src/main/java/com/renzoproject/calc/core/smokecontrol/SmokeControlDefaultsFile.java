package com.renzoproject.calc.core.smokecontrol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Shape of {@code reference/smoke-control/defaults.json}. Package-private -- internal to
 * {@link JsonSmokeControlDefaultsResolver}'s JSON parsing. {@code description} is present in the
 * JSON but not modeled here and is skipped via {@code @JsonIgnoreProperties}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SmokeControlDefaultsFile(DefaultsSection defaults) {

}
