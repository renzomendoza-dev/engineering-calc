package com.renzoproject.calc.core.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Shape of one entry under {@code air-properties.json}'s {@code properties} object.
 * Package-private -- internal to {@link JsonAirPropertiesResolver}'s JSON parsing.
 * {@code symbol}/{@code unit}/{@code confidence}/{@code source} are present in the JSON but not
 * modeled here -- transcription bookkeeping only -- and skipped via {@code @JsonIgnoreProperties}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record AirPropertyEntry(double value) {

}
