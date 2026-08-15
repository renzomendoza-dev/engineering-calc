package com.renzoproject.calc.core.acoustics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Hard ceiling from the {@code systemWideLimits} section of
 * {@code reference/acoustics/nfpa72-audibility-thresholds.json}, deserialized directly from that
 * JSON shape. {@code sourceSection} and {@code confidence} are present in the JSON but not
 * modeled here and are skipped via {@code @JsonIgnoreProperties}.
 *
 * @param maximumAllowedDba ceiling across all notification modes, exists to prevent hearing
 *                          damage -- a calculated target above this is a design conflict, not a
 *                          simple failure
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SystemWideLimits(double maximumAllowedDba) {

}
