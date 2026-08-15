package com.renzoproject.calc.core.smokecontrol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Shape of one entry under {@code defaults.json}'s {@code defaults} object. Package-private --
 * internal to {@link JsonSmokeControlDefaultsResolver}'s JSON parsing. {@code symbol}/
 * {@code unit}/{@code confidence}/{@code source}/{@code overridable} are present in the JSON but
 * not modeled here -- transcription bookkeeping only -- and skipped via
 * {@code @JsonIgnoreProperties}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record DefaultEntry(double value) {

}
